package it.gamems.game_service.service;

import it.gamems.game_service.client.WalletClient;
import it.gamems.game_service.dto.GameHistoryItemDto;
import it.gamems.game_service.dto.GamePlayResponseDto;
import it.gamems.game_service.dto.PlayRequestDto;
import it.gamems.game_service.entity.Game;
import it.gamems.game_service.entity.GameConfig;
import it.gamems.game_service.enums.GameStatus;
import it.gamems.game_service.event.GameEventPublisher;
import it.gamems.game_service.event.GameResultEventDto;
import it.gamems.game_service.exception.ExternalServiceException;
import it.gamems.game_service.exception.GameOperationException;
import it.gamems.game_service.repository.GameConfigRepository;
import it.gamems.game_service.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.security.SecureRandom;
import java.util.stream.Collectors;

/**
 * ========================================================
 * SERVICE: GameService
 * ========================================================
 * Il motore di gioco del microservizio. 
 * Gestisce l'algoritmo di lancio della moneta e l'orchestrazione 
 * dei servizi Wallet e Messaging.
 * * Uso di secure random per evitare calcoli deterministici
 * sulla generazione pseudo-casuale
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final String CONFIG_KEY = "COIN_FLIP_STATUS";
    
    private final GameRepository gameRepository;
    private final GameConfigRepository gameConfigRepository;
    private final GamePersistenceService persistenceService;
    private final WalletClient walletClient;
    private final GameEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRepository gameRepository, GameConfigRepository gameConfigRepository,
                        WalletClient walletClient, GamePersistenceService persistenceService, 
                       GameEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.persistenceService = persistenceService;
        this.walletClient = walletClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * ADMIN: Attiva o disattiva il gioco.
     */
    @Transactional
    public void setGameStatus(boolean active) {
        GameConfig config = gameConfigRepository.findById(CONFIG_KEY)
                .orElse(new GameConfig(CONFIG_KEY, true));
        
        config.setActive(active);
        gameConfigRepository.save(config);
    }

    /**
     * UTENZA: Verifica se il gioco è attivo.
     */
    @Transactional(readOnly = true)
    public boolean isGameActive() {
        return gameConfigRepository.findById(CONFIG_KEY)
                .map(GameConfig::isActive)
                .orElse(true);
    }

    /**
     * Esegue la logica della giocata.
     * 0. Controlla che il gioco sia active
     * 1. Sottrae i fondi (Sincrono)
     * 2. Lancia la moneta (Logica Interna)
     * 3. Salva l'esito (Database Locale)
     * 4. Invia la vincita se necessaria (Asincrono)
     */
    public GamePlayResponseDto play(String userId, PlayRequestDto request) {

        if (!isGameActive()) {
        throw new GameOperationException("Il gioco è attualmente in manutenzione. Riprova più tardi.");}

        log.info("Inizio giocata per utente [{}]: {}€ su {}", userId, request.betAmount(), request.choice());

        // Logica del lancio della moneta (Motore Probabilistico 49/51)
        String winningSide = calculateWinningSide(request.choice());
        boolean hasWon = request.choice().equalsIgnoreCase(winningSide);
        // Calcolo della vincita (o perdita :P)
        BigDecimal winAmount = hasWon ? 
                request.betAmount().multiply(new BigDecimal("2.00")) : 
                BigDecimal.ZERO;

        // 3. Persistenza dello storico su game_db
        Game game = new Game();
        game.setUserId(userId);
        game.setBetAmount(request.betAmount());
        game.setUserChoice(request.choice());
        game.setWinningSide(winningSide);
        game.setHasWon(hasWon);
        game.setWinAmount(winAmount);
        // Salvataggio "istantaneo" dello stato della partita (PENDING)
        game = persistenceService.createPendingGame(game);

        try {
            // Chiamata sincrona al Wallet per scalare i fondi.
            walletClient.debitWallet(userId, request.betAmount(), game.getId());
            
        } catch (GameOperationException e) {
            // Gestione: Il Wallet ha risposto, ma ha rifiutato l'operazione (es. Saldo insufficiente)
            log.warn("Giocata rifiutata per l'utente [{}]: {}. Marco la partita [{}] come FAILED.", 
                    userId, e.getMessage(), game.getId());
            persistenceService.updateGameStatus(game.getId(), GameStatus.FAILED);
            throw e; // Rilanciamo: Il GlobalExceptionHandler restituirà un 400 Bad Request
            
        } catch (Exception e) {
            // CASO B: TIMEOUT O ERRORE DI SISTEMA
            // 'Exception' fa da rete a strascico catturando sia ExternalServiceException 
            // che qualsiasi altro errore imprevisto a runtime.
            log.error("Disconnessione o Errore Critico per l'utente [{}]. Marco la partita [{}] come FAILED ed EMETTO EVENTO DI RIMBORSO.", 
                    userId, game.getId(), e);
            
            persistenceService.updateGameStatus(game.getId(), GameStatus.FAILED);
            
            // FONDAMENTALE: Diciamo subito al Wallet "Se ti sono arrivati i soldi per questa giocata, restituiscili!"
            eventPublisher.publishRefundRequest(game.getId(), userId, request.betAmount());
            
            // Rilanciamo l'eccezione originale se è una di quelle previste,
            // altrimenti la incapsuliamo in una RuntimeException genitore.
            if (e instanceof ExternalServiceException) {
                throw (ExternalServiceException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("Errore di sistema critico.", e);
            }
        }

        // Se arriviamo qui, i soldi sono stati scalati con successo.
        // Passiamo la partita allo stato definitivo.
        persistenceService.updateGameStatus(game.getId(), GameStatus.COMPLETED);

        // Se l'utente ha vinto, notifichiamo il Wallet in asincrono tramite RabbitMQ
        // dobbiamo "restituirgli" i soldi già prelevati in aticipo insieme alla vincita
        if (hasWon) {
            eventPublisher.publishGameResult(new GameResultEventDto(game.getId(), userId, true, winAmount));
        }

        String feedbackMessage = hasWon ? 
                "Complimenti! È uscito " + winningSide + ". Hai vinto " + winAmount + "€!" : 
                "Peccato, è uscito " + winningSide + ". Ritenta! Sarai più fortunato!";

        return new GamePlayResponseDto(
                game.getId(),
                game.getUserChoice(),
                game.getWinningSide(),
                game.isHasWon(),
                game.getBetAmount(),
                game.getWinAmount(),
                feedbackMessage
        );
    }

    /**
     * Recupera lo storico delle partite dell'utente.
     */
    @Transactional(readOnly = true)
    public List<GameHistoryItemDto> getHistory(String userId) {
        return gameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(game -> new GameHistoryItemDto(
                        game.getId(),
                        game.getUserChoice(),
                        game.getWinningSide(),
                        game.isHasWon(),
                        game.getBetAmount(),
                        game.getWinAmount(),
                        game.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Implementazione della probabilità 49% Utente / 51% Banco.
     * @param userChoice La faccia scelta dall'utente ("TESTA" o "CROCE")
     * @return La faccia che deve risultare vincente per rispettare le probabilità.
     */
    private String calculateWinningSide(String userChoice) {
        // Genera un numero casuale tra 0.000... e 0.999...
        double result = random.nextDouble(); 
        
        if (result < 0.49) {
            // L'UTENTE VINCE (49% dei casi)
            // Affinché l'utente vinca, deve "uscire" esattamente ciò che ha scelto
            return userChoice.toUpperCase();
        } else {
            // IL BANCO VINCE (51% dei casi)
            // Affinché l'utente perda, deve "uscire" la faccia opposta
            return userChoice.equalsIgnoreCase("TESTA") ? "CROCE" : "TESTA";
        }
    }
}