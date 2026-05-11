package it.gamems.api_gateway.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import it.gamems.api_gateway.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RateLimitingFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final AppConfig appConfig;

    public RateLimitingFilter(LettuceBasedProxyManager<byte[]> proxyManager, AppConfig appConfig) {
        this.proxyManager = proxyManager;
        this.appConfig = appConfig;
    }

    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> rateLimiter() {
        return (request, next) -> {

            String identifier;
            BucketConfiguration bucketConfig;

            // 1. Identifichiamo l'utente (Loggato vs Anonimo)
            // L'header X-User-Id viene iniettato dal JwtAuthFilter se il token è valido
            String userId = request.headers().firstHeader("X-User-Id");

            if (userId != null) {
                // Utente Loggato
                identifier = "rate_limit:user:" + userId;
                bucketConfig = createBucketConfig(appConfig.getRateLimit().getAuthenticated().getRequests(),
                                                  appConfig.getRateLimit().getAuthenticated().getDurationSec());
            } else {
                // Utente Anonimo (es. pagina di login)
                // Se c'è un reverse proxy (es. Nginx), prendiamo l'IP reale
                String ip = request.headers().firstHeader("X-Forwarded-For");
                if (ip == null) {
                    ip = request.remoteAddress().map(addr -> addr.getAddress().getHostAddress()).orElse("unknown-ip");
                }
                identifier = "rate_limit:ip:" + ip;
                bucketConfig = createBucketConfig(appConfig.getRateLimit().getAnonymous().getRequests(),
                                                  appConfig.getRateLimit().getAnonymous().getDurationSec());
            }

            // 2. Chiediamo a Redis il secchiello di questo identificatore (se non c'è, lo crea)
            Bucket bucket = proxyManager.builder().build(identifier.getBytes(), () -> bucketConfig);
            // 3. Proviamo a consumare 1 gettone
            if (bucket.tryConsume(1)) {
                // Gettone consumato con successo: la richiesta prosegue
                return next.handle(request);
            } else {
                // Secchio vuoto: Limite superato!
                log.warn("Rate limit superato per: {}", identifier);
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "code", "TOO_MANY_REQUESTS",
                                "message", "Hai effettuato troppe richieste. Rallenta un attimo!"
                        ));
            }
        };
    }

    /**
     * Metodo helper per creare la configurazione del secchiello
     */
    private BucketConfiguration createBucketConfig(int capacity, int durationSeconds) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity)
                        .refillGreedy(capacity, Duration.ofSeconds(durationSeconds)))
                .build();
    }
}