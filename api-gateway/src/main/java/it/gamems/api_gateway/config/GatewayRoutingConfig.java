package it.gamems.api_gateway.config;

import it.gamems.api_gateway.security.ApiKeyFilter;
import it.gamems.api_gateway.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

// IMPORT STATICI ESSENZIALI PER LA SINTASSI FLUENT MVC
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;

/**
 * ========================================================
 * CONFIGURAZIONE: GatewayRoutingConfig (Sintassi MVC Standard)
 * ========================================================
 * Questa classe si occupa di orchestrare le chiamate che arrivano
 * prima costruendo l'indirizzo di destinazione, poi passando le
 * richieste ai due filtri.
 * *Mappatura programmatica type-safe di tutto il cluster GameMs.
 * L'URL di destinazione viene iniettato come attributo della richiesta 
 * tramite la funzione prima del routing: .before(uri(URI.create(...)))
 */
@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes(ApiKeyFilter apiKeyFilter, JwtAuthFilter jwtAuthFilter) {

        return route("user-auth-service")
                .route(RequestPredicates.path("/api/v1/auth/**"), http())
                .before(uri(URI.create("http://user-service:8081")))
                .filter(apiKeyFilter.apiKeyAuth())
                .build()

            // --- SEZIONE ADMIN (Ordine Specifico -> Generico) ---

            .and(route("game-admin-service")
                .route(RequestPredicates.path("/api/v1/admin/game/**"), http())
                .before(uri(URI.create("http://game-service:8083")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build())

            .and(route("wallet-admin-service")
                .route(RequestPredicates.path("/api/v1/admin/wallets/**"), http())
                .before(uri(URI.create("http://wallet-service:8082")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build())

            .and(route("user-admin-service")
                .route(RequestPredicates.path("/api/v1/admin/**"), http())
                .before(uri(URI.create("http://user-service:8081")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build())

            // --- SEZIONE UTENTI REGOLARI ---

            .and(route("user-profile-service")
                .route(RequestPredicates.path("/api/v1/users/**"), http())
                .before(uri(URI.create("http://user-service:8081")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build())

            .and(route("game-service")
                .route(RequestPredicates.path("/api/v1/game/**"), http())
                .before(uri(URI.create("http://game-service:8083")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build())

            .and(route("wallet-service")
                .route(RequestPredicates.path("/api/v1/wallet/**"), http())
                .before(uri(URI.create("http://wallet-service:8082")))
                .filter(apiKeyFilter.apiKeyAuth())
                .filter(jwtAuthFilter.jwtAuth())
                .build());
    }
}