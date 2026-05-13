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
public class GameRefundListener {

    private static final Logger log = LoggerFactory.getLogger(GameRefundListener.class);
    private final WalletService walletService;

    public GameRefundListener(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * LISTENER DI COMPENSAZIONE: Gestisce i rimborsi automatici.
     * Si mette in ascolto sulla coda dei rimborsi alimentata dal GameService.
     */
    @RabbitListener(queues = RabbitMQConfig.REFUND_QUEUE_NAME)
    public void handleRefundRequest(RefundRequestEventDto event) {
        
        log.warn("📥 RICEVUTA RICHIESTA DI COMPENSAZIONE: Partita #{} per l'utente [{}]. Importo da verificare: {}€", 
                event.matchId(), event.userId(), event.amount());

        try {
            // Chiamata al metodo del Service che abbiamo blindato con il Libro Mastro
            walletService.processRefund(event.matchId(), event.userId(), event.amount());
            
        } catch (DataIntegrityViolationException e) {
            // ERRORE FATALE: Se i dati nel messaggio sono corrotti (es. ID null)
            log.error("Dati di rimborso non validi per la partita [{}]. Messaggio scartato in DLQ.", event.matchId());
            throw new AmqpRejectAndDontRequeueException("Refund data invalid", e);
            
        } catch (Exception e) {
            // ERRORE TRANSITORIO: Es: Database temporaneamente occupato
            log.warn("Errore temporaneo durante il rimborso della partita [{}]. Riprovo...", event.matchId());
            throw new RuntimeException("Retry rimborso", e);
        }
    }
}