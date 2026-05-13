package it.gamems.game_service.client;

import it.gamems.game_service.config.AppConfig;
import it.gamems.game_service.exception.ExternalServiceException;
import it.gamems.game_service.exception.GameOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * ========================================================
 * CLIENT: WalletClient
 * ========================================================
 * Gestisce le chiamate REST verso il microservizio Wallet.
 * Utilizza RestClient, il nuovo client sincrono di Spring Boot,
 * ideale per lavorare con i Virtual Threads di Java 25.
 */
@Component
public class WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);
    private final RestClient restClient;
    private final AppConfig appConfig;

    public WalletClient(RestClient.Builder builder, AppConfig appConfig) {

        this.appConfig=appConfig;
        
        // Connection timeout verso wallet-service a 3 sec.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        // Traduzione del client nativo in una factory compatibile con spring
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Read timeout a 3 secondi
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        // Costruiamo il RestClient finale
        this.restClient = builder
                .baseUrl(appConfig.getWalletService().getUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Chiama l'endpoint del Wallet per scalare i soldi della puntata.
     * @param userId L'ID dell'utente che sta giocando.
     * @param amount L'importo da scalare.
     * @param matchId L'ID della partita (Game)
     * Iniezione api-key interna
     * @throws RuntimeException se il saldo è insufficiente o il servizio è offline.
     */
    public void debitWallet(String userId, BigDecimal amount, Long matchId) {
        log.info("Chiamata sincrona al Wallet per utente [{}]: debito di {}€", userId, amount);

        try {
            restClient.post()
                    .uri("/api/v1/internal/wallet/bet")
                     // Passiamo l'ID utente ricevuto dal Gateway
                    .header("X-User-Id", userId)
                    // Inezione Api-Key interna
                    .header("X-Internal-Secret", appConfig.getInternal().getSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new WalletBetDto(amount, matchId))
                    .retrieve()
                    // Se il Wallet risponde con 4xx o 5xx, viene lanciata un'eccezione
                    .onStatus(status -> status.isError(), (request, response) -> {
                        log.error("Il Wallet Service ha rifiutato la giocata. Status: {}", response.getStatusCode());
                        throw new GameOperationException("Impossibile procedere con la giocata: saldo insufficiente o errore tecnico.");
                    })
                    .toBodilessEntity();

            log.debug("Debito confermato dal Wallet per l'utente [{}]", userId);

        } catch (GameOperationException e) {
            // Se è già un'eccezione di business, la facciamo passare intatta verso il controller
            throw e;
        } catch (Exception e) {
            log.error("Errore di rete o timeout durante la comunicazione con il Wallet: {}", e.getMessage());
            throw new ExternalServiceException("Servizio Wallet non raggiungibile. Riprova più tardi.");
        }
    }
}