package it.gamems.user_service.event;

import it.gamems.user_service.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Invia un evento di sicurezza all'API Gateway.
     */
    public void publishSecurityEvent(Long userId, String action) {
        log.info("Pubblicazione evento di sicurezza: Azione [{}] per Utente [{}]", action, userId);

        try {
            SecurityEventDto eventDto = new SecurityEventDto(userId, action);
            
            // Nel Fanout, la routing key (il secondo parametro) viene ignorata
            rabbitTemplate.convertAndSend(RabbitMQConfig.SECURITY_EXCHANGE, "", eventDto);
            
            log.debug("Evento di sicurezza inoltrato con successo.");
        } catch (Exception e) {
            // Logghiamo l'errore senza far crashare il thread principale.
            // L'utente risulterà bannato sul DB, ma il Gateway non riceverà l'avviso istantaneo.
            log.error("Fallimento critico nella notifica di sicurezza per utente [{}]: {}", userId, e.getMessage(), e);
        }
    }
}