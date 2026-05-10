package it.gamems.api_gateway.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String SECURITY_EXCHANGE = "security.fanout";

    @Bean
    public FanoutExchange securityExchange() { return new FanoutExchange(SECURITY_EXCHANGE); }

    @Bean
    public Queue autoDeleteQueue() { return new AnonymousQueue(); }

    @Bean
    public Binding securityBinding(FanoutExchange securityExchange, Queue autoDeleteQueue) {
        return BindingBuilder.bind(autoDeleteQueue).to(securityExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}