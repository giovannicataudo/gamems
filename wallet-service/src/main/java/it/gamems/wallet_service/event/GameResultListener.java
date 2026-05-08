package it.gamems.wallet_service.event;

import it.gamems.wallet_service.config.RabbitMQConfig;
import it.gamems.wallet_service.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
            
        } catch (Exception e) {
            /*
             * GESTIONE ERRORI ASINCRONI:
             * Se il database crasha qui, non possiamo restituire un 500 all'utente 
             * perché l'utente non sta aspettando (la richiesta HTTP è già chiusa).
             * Loggiamo l'errore per un eventuale retry manuale o per instradare 
             * il messaggio in una Dead Letter Queue (DLQ).
             */
            log.error("Errore critico durante l'accredito della vincita per l'utente [{}]. Il messaggio verrà spostato nella DLQ. Dettaglio errore: {}", 
            event.userId(), e.getMessage(), e);
            
            // Questa eccezione ordina a RabbitMQ di NON rimettere il messaggio nella coda originaria.
            // Poiché in RabbitMQConfig abbiamo configurato un "x-dead-letter-exchange", 
            // RabbitMQ intercetterà questo rifiuto e instraderà il messaggio nella DLQ in modo sicuro.
            throw new AmqpRejectAndDontRequeueException("Impossibile processare il messaggio. Spostamento in DLQ.", e);
        }
    }
}