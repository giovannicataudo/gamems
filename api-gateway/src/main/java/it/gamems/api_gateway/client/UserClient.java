package it.gamems.api_gateway.client;

import it.gamems.api_gateway.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);
    private final RestClient restClient;
    private final AppConfig appConfig;

    public UserClient(RestClient.Builder builder, AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restClient = builder.baseUrl(appConfig.getUserService().getUrl()).build();
    }

    public List<Long> fetchBannedUserIds() {
        log.info("Richiesta lista utenti bannati allo User Service...");
        try {
            return restClient.get()
                    .uri("/api/v1/internal/users/banned-ids")
                    .header("X-Internal-Secret", appConfig.getInternal().getSecret())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Errore critico nel recupero della blacklist: {}", e.getMessage());
            return List.of(); // Ritorna lista vuota per evitare crash all'avvio
        }
    }
}