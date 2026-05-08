package it.gamems.wallet_service.client;

import it.gamems.wallet_service.exception.PaymentGatewayException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/**
 * ========================================================
 * CLIENT / ADAPTER: PaymentGatewayClient
 * ========================================================
 * Gestisce esclusivamente l'interazione con il mondo esterno 
 * (il fittizio provider di pagamenti). 
 * Isola la logica di rete, latenza e retry dal core business.
 */
@Component
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    private final Random random = new Random();

    /**
     * Simula l'elaborazione di un pagamento presso un gateway esterno.
     * @param amount L'importo da ricaricare
     * @throws RuntimeException se il gateway esterno fallisce.
     */
    public void processPayment(BigDecimal amount) {
        log.info("Iniziata comunicazione con il Gateway di Pagamento per l'importo di {}€...", amount);
        
        try {
            // Sfruttiamo i Virtual Threads di Java 25: 
            // Thread.sleep sospende il thread virtuale, ma NON blocca la CPU.
            long delay = 500 + random.nextInt(1000);
            Thread.sleep(delay); 
            
            // Tasso di fallimento fisiologico del 5% tipico dei gateway reali
            if (random.nextInt(100) < 5) {
                log.error("Il Gateway di Pagamento esterno non ha risposto (Simulazione Timeout 503).");
                throw new PaymentGatewayException("Gateway di pagamento non raggiungibile in questo momento. Riprova.");
            }
            
            log.info("Transazione di {}€ approvata dal Gateway.", amount);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Processo di comunicazione di rete interrotto.");
        }
    }
}