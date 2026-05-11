package it.gamems.wallet_service.worker;

import it.gamems.wallet_service.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * ========================================================
 *  WORKER: DlqMaintenanceWorker
 * ======================================================== 
 * Questo componente agisce in background in modo asincrono rispetto al traffico web.
 * Il suo scopo è ispezionare la Dead Letter Queue (DLQ) una volta al giorno.
 * * LOGICA DI RECOVERY:
 * - Se un messaggio è al suo PRIMO fallimento (x-death count = 1), viene rimesso 
 * nella coda principale per un secondo tentativo.
 * - Se fallisce di nuovo e torna qui (x-death count > 1), viene scartato 
 * definitivamente per evitare loop infiniti (Poison Pill), e il suo payload 
 * viene stampato nei log per permettere l'intervento manuale del Customer Care.
 */
@Component
public class DlqMaintenanceWorker {

    private static final Logger log = LoggerFactory.getLogger(DlqMaintenanceWorker.class);
    private final RabbitTemplate rabbitTemplate;

    public DlqMaintenanceWorker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Esegue il task ogni notte alle 03:00.
     * La sintassi Cron è: "secondo minuto ora giorno_mese mese giorno_settimana"
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void processDlqOnce() {
        log.info("[WORKER] Avvio manutenzione notturna della Dead Letter Queue (DLQ)...");
        
        int requeuedCount = 0;
        int discardedCount = 0;

        // Ciclo continuo finché non svuotiamo la DLQ
        while (true) {
            // Estraiamo il messaggio. Se la coda è vuota, receive() restituisce null.
            Message message = rabbitTemplate.receive(RabbitMQConfig.DLQ_NAME);
            if (message == null) {
                break; // Coda svuotata, usciamo dal ciclo
            }

            MessageProperties properties = message.getMessageProperties();
            long deadLetterCount = getDeathCount(properties);

            if (deadLetterCount == 1) {
                /*
                 * CASO 1: Primo fallimento.
                 * Rimettiamo il messaggio nell'Exchange principale.
                 */
                log.debug("[WORKER] Trovato messaggio al primo fallimento. Rimetto in coda per il ripristino.");
                rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);
                requeuedCount++;
                
            } else {
                /*
                 * CASO 2: Fallimenti multipli (Poison Pill).
                 * Il messaggio ha fallito anche il retry notturno. Deve essere scartato.
                 * Estraiamo il JSON dal body per non perdere traccia dei soldi dell'utente.
                 */
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                
                log.error("=======================================================================");
                log.error("[ALLARME DLQ] POISON PILL SCARTATA DEFINITIVAMENTE!");
                log.error("Il messaggio ha fallito {} tentativi. Intervento manuale richiesto.", deadLetterCount);
                log.error("PAYLOAD DELLA GIOCATA PERSA: {}", payload);
                log.error("=======================================================================");
                
                discardedCount++;
            }
        }

        // Report finale di fine esecuzione
        if (requeuedCount > 0 || discardedCount > 0) {
            log.info("[WORKER] Report DLQ Notturno concluso: {} messaggi riprovati, {} scartati per intervento manuale.", 
                    requeuedCount, discardedCount);
        } else {
            log.info("[WORKER] Manutenzione conclusa. La DLQ era vuota. Tutto regolare.");
        }
    }

    /**
     * Metodo helper per estrarre in modo sicuro il contatore nativo 'x-death' 
     * iniettato automaticamente da RabbitMQ quando sposta un messaggio in DLQ.
     */
    private long getDeathCount(MessageProperties properties) {
        List<Map<String, ?>> xDeath = properties.getXDeathHeader();
        
        if (xDeath != null && !xDeath.isEmpty()) {
            // L'elemento all'indice 0 contiene i dati dell'ultimo evento di dead-lettering
            Object count = xDeath.get(0).get("count");
            
            // RabbitMQ solitamente lo salva come Long, ma facciamo un controllo 
            // incrociato con Integer per evitare fastidiose ClassCastException.
            if (count instanceof Long) {
                return (Long) count;
            } else if (count instanceof Integer) {
                return ((Integer) count).longValue();
            }
        }
        
        // Fallback di sicurezza: se per qualche motivo l'header manca, 
        // assumiamo sia il primo fallimento per dargli almeno una chance.
        return 1L; 
    }
}