package it.gamems.api_gateway.security;

import it.gamems.api_gateway.config.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * ========================================================
 * FILTER: ApiKeyFilter (Client Authentication)
 * ========================================================
 * Controlla che chiunque stia chiamando il Gateway possieda 
 * la chiave segreta dell'applicazione.
 * Serve per impedire ad hacker o bot di tempestare le nostre API 
 * scavalcando il nostro Frontend ufficiale.
 */
@Configuration
public class ApiKeyFilter {

    private final AppConfig appConfig;

    public ApiKeyFilter(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> apiKeyAuth() {
        return (request, next) -> {
            
            // 1. Estrazione dell'header personalizzato
            String requestApiKey = request.headers().firstHeader("X-Api-Key");

            // 2. Controllo: Manca la chiave o è sbagliata?
            if (requestApiKey == null || !requestApiKey.equals(appConfig.getApiKey())) {
                // Restituiamo 403 FORBIDDEN (Rifiutato dal server per mancanza di permessi base)
                return ServerResponse.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. Se la chiave è corretta, lasciamo passare la richiesta 
            // al filtro successivo (es. JwtAuthFilter) o alla rotta finale.
            return next.handle(request);
        };
    }
}