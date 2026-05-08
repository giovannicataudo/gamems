package it.gamems.game_service.event;

import it.gamems.game_service.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * ========================================================
 * PUBLISHER: GameEventPublisher
 * ========================================================
 * Questo componente funge da "postino" per il microservizio.
 * Isola la dipendenza da RabbitMQ (RabbitTemplate) dal resto 
 * della logica di business.
 */
@Component
public class GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GameEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    // Iniezione tramite costruttore della classe core di Spring AMQP
    public GameEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Invia l'evento di fine partita al broker.
     * * @param eventDto Il payload contenente i dati della vincita da inviare al Wallet.
     */
    public void publishGameResult(GameResultEventDto eventDto) {
        log.info("Pubblicazione evento RabbitMQ: Utente [{}] ha vinto? {}. Importo: {}€", 
                eventDto.userId(), eventDto.hasWon(), eventDto.winAmount());

        try {
            // convertAndSend trasforma automaticamente il Record Java in JSON 
            // grazie al JacksonJsonMessageConverter che abbiamo definito in RabbitMQConfig
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME, 
                    RabbitMQConfig.ROUTING_KEY, 
                    eventDto
            );
            
            log.debug("Evento inviato con successo all'exchange '{}' con routing key '{}'", 
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY);
                    
        } catch (Exception e) {
            // Logghiamo l'errore ma NON blocchiamo il thread. 
            // Se RabbitMQ è momentaneamente irraggiungibile, non vogliamo far crashare l'app.
            // In un ambiente di produzione avanzato, qui si implementerebbe il pattern "Outbox"
            // salvando il messaggio su un database per riprovare in seguito.
            log.error("ATTENZIONE: Fallimento critico nella pubblicazione dell'evento per l'utente [{}]: {}", 
                    eventDto.userId(), e.getMessage(), e);
        }
    }
}