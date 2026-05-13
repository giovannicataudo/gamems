package it.gamems.game_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ========================================================
 * CONFIGURAZIONE: RabbitMQConfig (Producer)
 * ========================================================
 * Prepara il microservizio per inviare messaggi sul broker.
 */
@Configuration
public class RabbitMQConfig {

    // Devono combaciare ESATTAMENTE con quelli definiti nel wallet-service
    public static final String EXCHANGE_NAME = "game.exchange";
    public static final String ROUTING_KEY = "game.result.wallet";
    public static final String REFUND_ROUTING_KEY = "game.refund.routing";

    /**
     * Dichiara l'Exchange. Se non esiste sul broker, Spring lo creerà all'avvio.
     */
    @Bean
    public TopicExchange gameExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * CONVERTITORE JSON.
     * Utilizziamo la classe aggiornata (JacksonJsonMessageConverter) per 
     * trasformare i nostri Record Java in payload JSON validi per la rete.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}