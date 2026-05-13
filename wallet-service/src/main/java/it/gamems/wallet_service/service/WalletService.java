package it.gamems.wallet_service.service;

import it.gamems.wallet_service.client.PaymentGatewayClient;
import it.gamems.wallet_service.dto.WalletAdjustmentRequestDto;
import it.gamems.wallet_service.dto.WalletResponseDto;
import it.gamems.wallet_service.entity.ProcessedEvent;
import it.gamems.wallet_service.entity.Wallet;
import it.gamems.wallet_service.entity.WalletTransaction;
import it.gamems.wallet_service.exception.WalletOperationException;
import it.gamems.wallet_service.repository.ProcessedEventRepository;
import it.gamems.wallet_service.repository.WalletRepository;
import it.gamems.wallet_service.repository.WalletTransactionRepository;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;

import java.util.Optional;
import java.math.BigDecimal;

/**
 * ========================================================
 * SERVICE: WalletService
 * ========================================================
 * Il "cervello" finanziario del microservizio. 
 * Gestisce la logica di business, le transazioni e le regole AML.
 * @Transactional garantisce che tutte le operazioni sul database 
 * eseguite all'interno di quel metodo avvengano all'interno di una 
 * singola transazione del database. In caso di eccezione Spring
 * esegue un rollback in automatico.
 * * * LOGICA DI PRELIEVO FONDI (Playthrough 1x):
 * Quando un utente scommette, il sistema sottrae i fondi prima dal 
 * 'realBalance' (deposito). Solo se questo è zero, attinge dal 
 * 'withdrawableBalance' (vincite).
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PaymentGatewayClient paymentClient;

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    public WalletService(WalletRepository walletRepository, PaymentGatewayClient paymentClient,
        ProcessedEventRepository processedEventRepository,
        WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.paymentClient = paymentClient;
        this.processedEventRepository = processedEventRepository;
    }

    /**
    * Cerca il wallet di un utente. 
    * Se esiste lo restituisce, altrimenti lancia un'eccezione di business.
    */
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletOrThrow(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new WalletOperationException("Portafoglio non trovato per l'utente: " + userId));

        // Mappiamo l'entità nel DTO di risposta
        return new WalletResponseDto(
                wallet.getRealBalance(),
                wallet.getWithdrawableBalance(),
                null // Il totale viene calcolato automaticamente dal costruttore del Record
        );
    }

    /**
     * Recupera il saldo dell'utente e lo converte in un DTO.
     * Se l'utente non ha un portafoglio (primo accesso), lo crea automaticamente.
     */
    @Transactional
    public WalletResponseDto getWalletStatus(String userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return new WalletResponseDto(
                wallet.getRealBalance(),
                wallet.getWithdrawableBalance(),
                // Il calcolo del totale è delegato al costruttore del DTO
                null
        );
    }

    /**
     * SIMULAZIONE DEPOSITO (Pagamento Esterno)
     * Implementa una chiamata fittizia a un provider di pagamento.
     */
    public void deposit(String userId, BigDecimal amount) {
        
        // Deleghiamo il carico di rete al Client dedicato.
        // Se il pagamento fallisce, lancia un'eccezione e il flusso si blocca qui.
        paymentClient.processPayment(amount);

        // Chiamata al Database (metodo di sotto)
        executeDepositTransaction(userId, amount);
    }
    // Separando la chiamata mentre un user attende il tempo della ricarica
    // non viene occupato un pool di connessione al db
    @Transactional
    public void executeDepositTransaction(String userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setRealBalance(wallet.getRealBalance().add(amount));
        walletRepository.save(wallet);
    }

    /**
     * LOGICA CORE: Detrazione fondi per la giocata.
     * Questa operazione verrà chiamata dal GameService prima di lanciare la moneta.
     * @Retryable intercetta i conflitti di concorrenza (Lock Ottimistico).
     * Se due thread provano a modificare il saldo nello stesso millisecondo, 
     * il thread sconfitto aspetterà 100ms e riproverà automaticamente, 
     * fino a un massimo di 3 tentativi, evitando di restituire errore 500 all'utente.
     */
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class, 
            maxAttempts = 3, 
            backoff = @Backoff(delay = 100) // Attesa di 100 millisecondi tra un tentativo e l'altro
    )
    public void processGameBet(String userId, BigDecimal betAmount, Long matchId) {

        // 1. VERIFICA IDEMPOTENZA (Scudo anti-duplicati REST)
        // Se troviamo già una transazione di tipo DEBIT per questo matchId, ignoriamo.
        if (transactionRepository.findByMatchId(matchId).isPresent()) {
            log.warn("IDEMPOTENZA: Addebito per partita #{} già eseguito. Salto l'operazione.", matchId);
            return;
        }

        Wallet wallet = getOrCreateWallet(userId);
        
        BigDecimal totalAvailable = wallet.getRealBalance().add(wallet.getWithdrawableBalance());
        
        // Verifica se l'utente ha abbastanza fondi totali
        if (totalAvailable.compareTo(betAmount) < 0) {
            throw new WalletOperationException("Saldo insufficiente per effettuare la giocata");
        }

        // --- ALGORITMO DI SOTTRAZIONE PRIORITARIA ---
        BigDecimal remainingToSubtract = betAmount;

        // 1. Sottraiamo prima dal saldo reale (deposito)
        if (wallet.getRealBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal amountFromReal = wallet.getRealBalance().min(remainingToSubtract);
            wallet.setRealBalance(wallet.getRealBalance().subtract(amountFromReal));
            remainingToSubtract = remainingToSubtract.subtract(amountFromReal);
        }

        // 2. Se rimane ancora da sottrarre, attingiamo dal saldo prelevabile (vincite)
        if (remainingToSubtract.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().subtract(remainingToSubtract));
        }

        walletRepository.save(wallet);

        // 2. SCRITTURA LIBRO MASTRO (FONDAMENTALE per la Saga)
        // Registriamo il debito in modo che il sistema di rimborso possa rintracciarlo.
        transactionRepository.save(new WalletTransaction(userId, matchId, betAmount, "DEBIT"));

        log.info("Addebito confermato per partita #{}: {}€ scalati all'utente [{}].", matchId, betAmount, userId);
    }

    /**
     * NUOVO METODO: Compensazione Saga (Asincrona RabbitMQ).
     * Chiamato quando il GameService segnala un fallimento dopo l'invio della bet.
     */
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
    public void processRefund(Long matchId, String userId, BigDecimal amount) {
        log.info("Ricevuta richiesta di compensazione per partita #{}...", matchId);

        // 1. VERIFICA REALE NECESSITÀ
        // Controlliamo se avevamo effettivamente scalato i soldi.
        Optional<WalletTransaction> debitTx = transactionRepository.findByMatchId(matchId);

        if (debitTx.isEmpty()) {
            // Se non c'è il debito, non dobbiamo rimborsare nulla. 
            // Significa che la chiamata REST era fallita prima di toccare il saldo.
            log.info("Compensazione ignorata: nessun addebito trovato per la partita #{}.", matchId);
            return;
        }

        // 2. EVITIAMO DOPPI RIMBORSI
        if ("REFUNDED".equals(debitTx.get().getTransactionType())) {
            log.warn("La partita #{} è già stata rimborsata. Operazione ignorata.", matchId);
            return;
        }

        // 3. ESECUZIONE RIMBORSO
        Wallet wallet = getOrCreateWallet(userId);
        // Restituiamo i soldi nel saldo REALE (per semplicità e correttezza fiscale)
        wallet.setRealBalance(wallet.getRealBalance().add(amount));
        walletRepository.save(wallet);

        // 4. AGGIORNAMENTO STATO TRANSAZIONE (NON CANCELLARE!)
        // Modifichiamo il tipo di transazione per mantenere lo storico e bloccare futuri retry della REST
        WalletTransaction tx = debitTx.get();
        // Aggiungi il metodo setTransactionType nella tua entity WalletTransaction se non c'è
        tx.setTransactionType("REFUNDED"); 
        transactionRepository.save(tx);

        log.info("✅ COMPENSAZIONE COMPLETATA: Rimborsati {}€ per partita #{} all'utente [{}].", amount, matchId, userId);
    }

    /**
     * ACCREDITO VINCITA (Playthrough 1x completato)
     * Questo metodo verrà richiamato in modo asincrono (es. ascoltando RabbitMQ)
     * quando il Game Service decreta la vittoria dell'utente.
     */
    @Transactional
    public void processGameWin(Long matchId, String userId, BigDecimal winAmount) {

        // Controllo di Idempotenza (Scudo anti-duplicati)
        if (processedEventRepository.existsById(matchId)) {
            // Se la targa esiste già, usciamo silenziosamente.
            // NON lanciamo un'eccezione, altrimenti RabbitMQ penserebbe a un errore 
            // e rimetterebbe il messaggio in coda all'infinito.
            log.warn("IDEMPOTENZA TRIGGERED: Messaggio di vincita scartato. Partita [{}] già accreditata per l'utente [{}].", matchId, userId);
            return; 
        }
        // Registriamo la targa come "elaborata" per bloccare futuri duplicati
        processedEventRepository.save(new ProcessedEvent(matchId));

        Wallet wallet = getOrCreateWallet(userId);
        
        // Regola AML: L'intero ritorno economico della vincita 
        // va a rimpinguare ESCLUSIVAMENTE il saldo prelevabile.
        wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().add(winAmount));
        
        walletRepository.save(wallet);
    }

    /**
     * PRELIEVO VINCITE
     * L'utente può prelevare SOLO dal saldo delle vincite.
     */
    @Transactional
    public void withdraw(String userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getWithdrawableBalance().compareTo(amount) < 0) {
            throw new WalletOperationException("Puoi prelevare solo le tue vincite. Saldo prelevabile insufficiente.");
        }

        wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    /**
     * OPERAZIONE ADMIN: Rettifica manuale del saldo.
     * Permette di accreditare fondi specificando la tipologia di saldo.
     */
    @Transactional
    public void adjustWalletBalance(String userId, WalletAdjustmentRequestDto adjustment) {
        // 1. Recupero il wallet (o lo creo se non esiste, per resilienza)
        Wallet wallet = getOrCreateWallet(userId);

        // 2. Applico la modifica in base al tipo richiesto
        if ("REAL".equalsIgnoreCase(adjustment.balanceType())) {
            wallet.setRealBalance(wallet.getRealBalance().add(adjustment.amount()));
        } else if ("WITHDRAWABLE".equalsIgnoreCase(adjustment.balanceType())) {
            wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().add(adjustment.amount()));
        } else {
            throw new WalletOperationException("Tipo di saldo non valido: " + adjustment.balanceType());
        }

        // 3. Log di controllo (Audit)
        log.info("ADMIN ADJUSTMENT: Utente {}, Importo {}, Tipo {}, Motivo: {}", 
                 userId, adjustment.amount(), adjustment.balanceType(), adjustment.reason());

        walletRepository.save(wallet);
    }

    // Metodo getOrCreateWallet
    private Wallet getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUserId(userId);
                    newWallet.setRealBalance(BigDecimal.ZERO);
                    newWallet.setWithdrawableBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }
}