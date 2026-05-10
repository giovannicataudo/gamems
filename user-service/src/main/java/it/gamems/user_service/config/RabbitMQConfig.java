package it.gamems.user_service.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {

    // Il nome dell'Exchange dedicato alla sicurezza
    public static final String SECURITY_EXCHANGE = "security.fanout";

    // Dichiarazione o creazione dell'exchange
    @Bean
    public FanoutExchange securityExchange() {
        return new FanoutExchange(SECURITY_EXCHANGE);
    }

    // Convertitore in json
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}