package it.gamems.wallet_service.event;

import it.gamems.wallet_service.config.RabbitMQConfig;
import it.gamems.wallet_service.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * ========================================================
 * CONSUMER: GameResultListener
 * ========================================================
 * Componente sempre in ascolto sulla coda specificata.
 * Sfrutta i Virtual Threads (se abilitati a livello di factory AMQP) 
 * per elaborare decine di migliaia di vittorie al secondo senza bloccare la CPU.
 */
@Component
public class GameResultListener {

    private static final Logger log = LoggerFactory.getLogger(GameResultListener.class);
    private final WalletService walletService;

    public GameResultListener(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * CORE LOGIC ASINCRONA.
     * @RabbitListener mette il metodo in ascolto continuo.
     * @param event Il JSON convertito automaticamente da Spring.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleGameResult(GameResultEventDto event) {
        
        log.info("Ricevuto evento di fine partita per l'utente [{}]. Esito vittoria: {}", 
                event.userId(), event.hasWon());

        try {
            // Se l'utente ha vinto e l'importo è valido, accreditiamo i fondi.
            // (Se ha perso, i soldi sono già stati scalati sincronicamente prima del lancio).
            if (event.hasWon() && event.winAmount() != null) {
                
                walletService.processGameWin(event.matchId(), event.userId(), event.winAmount());
                
                log.info("Accreditata vincita di {}€ all'utente [{}] nel saldo prelevabile per la partita [{}].", 
                        event.winAmount(), event.userId(), event.matchId());
            }
            
        } catch (IllegalArgumentException | NullPointerException | DataIntegrityViolationException e) {
            // ERRORE FATALE STRUTTURALE (Poison Pill)
            // Es: Manca l'ID utente, dati corrotti. Riprovare 5 volte è inutile.
            // Qui USIAMO esplicitamente AmqpRejectAndDontRequeueException per bypassare i retry
            // e mandare subito il messaggio nella DLQ.
            log.error("Dati corrotti per l'evento [{}]. Spostamento istantaneo in DLQ.", event.matchId(), e);
            throw new AmqpRejectAndDontRequeueException("Dati non validi, impossibile accreditare", e);
            
        } catch (Exception e) {
            // ERRORE TRANSITORIO (Es: DB offline, Lock Ottimistico fallito)
            // NON lanciamo la RejectException! Rilanciamo l'errore originale.
            // Spring intercetterà l'errore, aspetterà 2 secondi e riproverà da solo (max 5 volte).
            // Se fallisce per la quinta volta, Spring lo manderà in DLQ per conto suo.
            log.warn("Errore temporaneo accreditamento partita [{}]. Tentativo di retry in corso...", event.matchId());
            throw new RuntimeException("Errore transitorio, lascio agire il retry di Spring", e);
        }
    }
}