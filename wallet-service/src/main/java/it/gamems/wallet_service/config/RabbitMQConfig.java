package it.gamems.wallet_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ========================================================
 * CONFIGURAZIONE: RabbitMQConfig
 * ========================================================
 * Definisce l'infrastruttura di messaggistica per il microservizio.
 * Spring Boot utilizzerà questi @Bean per creare automaticamente 
 * la coda sul broker RabbitMQ (se non esiste) e collegarla all'Exchange.
 * @Bean crea oggetti complessi e li mette a disposizione quando chiamati.
 */
@Configuration
public class RabbitMQConfig {

    // Nomi costanti per evitare errori di battitura
    public static final String QUEUE_NAME = "wallet.game.result.queue";
    public static final String EXCHANGE_NAME = "game.exchange";
    public static final String ROUTING_KEY = "game.result.wallet";

    // Nomi costanti per la DLQ (Dead Lettere Queue)
    public static final String DLQ_NAME = "wallet.game.result.dlq";
    public static final String DLX_NAME = "wallet.dlx";
    public static final String DLQ_ROUTING_KEY = "wallet.dlq.routing.key";

    // Costanti per la refunding
    public static final String REFUND_ROUTING_KEY = "game.refund.routing";
    public static final String REFUND_QUEUE_NAME = "wallet.refund.queue";

    /**
     * Crea la coda persistente (durable = true).
     * Se RabbitMQ si riavvia, i messaggi non elaborati non andranno persi.
     */
    @Bean
    public Queue walletQueue() {
        //Usiamo QueueBuilder per aggiungere gli argomenti della DLQ.
        return QueueBuilder.durable(QUEUE_NAME)
                // Se un messaggio viene rigettato, mandalo a questo Exchange
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                // Usando questa specifica etichetta
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Definisce l'Exchange (il "postino" di RabbitMQ) di tipo Topic.
     * Permette di instradare i messaggi in base a pattern di chiavi.
     */
    @Bean
    public TopicExchange gameExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Lega la coda all'Exchange tramite la Routing Key.
     * Significa: "Manda a QUEUE_NAME tutti i messaggi che arrivano 
     * su EXCHANGE_NAME con etichetta ROUTING_KEY".
     */
    @Bean
    public Binding binding(Queue walletQueue, TopicExchange gameExchange) {
        return BindingBuilder.bind(walletQueue).to(gameExchange).with(ROUTING_KEY);
    }

    /**
     * 1. Creazione della coda per i rimborsi.
     */
    @Bean
    public Queue refundQueue() {
        return QueueBuilder.durable(REFUND_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 2. Binding per i rimborsi.
     */
    @Bean
    public Binding refundBinding(Queue refundQueue, TopicExchange gameExchange) {
        return BindingBuilder.bind(refundQueue)
                .to(gameExchange)
                .with(REFUND_ROUTING_KEY);
    }

    /**
     * Definisce l'Exchange della DLQ
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    /**
     * Crea la queue della DLQ
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    /**
     * Lega la coda all'exchange tramite routing key
     */
    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    /**
     * CONVERTITORE JSON FONDAMENTALE (Aggiornato per Spring 4.0+).
     * Utilizza JacksonJsonMessageConverter al posto del deprecato Jackson2...
     * per trasformare automaticamente i JSON in arrivo nei Record Java.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}