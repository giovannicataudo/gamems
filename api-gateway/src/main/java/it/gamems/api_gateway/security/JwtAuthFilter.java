package it.gamems.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gamems.api_gateway.config.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * ========================================================
 * FILTER: JwtAuthFilter (Global Gateway Security)
 * ========================================================
 * Intercetta le richieste HTTP in ingresso prima che vengano 
 * instradate ai microservizi a valle (User, Game, Wallet).
 * * * * FONDAMENTALE:
 * Centralizzando la validazione del token qui, impediamo 
 * che richieste con JWT scaduti o manomessi consumino 
 * risorse di rete e CPU nei servizi di dominio.
 */
@Configuration
public class JwtAuthFilter {

    private final AppConfig appConfig;

    public JwtAuthFilter(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * CORE LOGIC: Validazione Token e Iniezione Header
     */
    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> jwtAuth() {
        return (request, next) -> {
            
            // 1. ESTRAZIONE DELL'HEADER
            // Recupera l'header "Authorization" dalla richiesta HTTP originale
            String authHeader = request.headers().firstHeader("Authorization");

            // Se l'header manca o non ha il formato "Bearer ", blocca la richiesta con 401
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Estrazione del token fisico (saltando i primi 7 caratteri: "Bearer ")
            String token = authHeader.substring(7);

            try {
                // 2. VALIDAZIONE CRITTOGRAFICA E LETTURA CLAIMS
                // Il parser di jjwt lancia un'eccezione se la firma non combacia o se è scaduto
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 3. PROPAGAZIONE DELL'IDENTITÀ (TRUSTED M2M)
                // Estraiamo l'ID utente dal payload del token e lo convertiamo in stringa
                String userId = claims.get("userId").toString();
                
                // NOTA CRITICA MVC: 
                // In WebMVC usiamo ServerRequest.from(request) per clonare la richiesta
                // in modo immutabile e aggiungere l'header "X-User-Id".
                // I controller a valle useranno @RequestHeader("X-User-Id") fidandosi del Gateway.
                ServerRequest modifiedRequest = ServerRequest.from(request)
                        .header("X-User-Id", userId)
                        .build();

                // Passiamo la richiesta modificata al blocco successivo
                // In questo caso la richiesta validata e pronta per essere
                // usata anche dai microservizi interni viene passata al
                // roxyExchange (o Forwarding Handler) che si occupa di 
                // passorlo poi all'url di destinazione (e quindi al servizio)
                return next.handle(modifiedRequest);

            } catch (Exception e) {
                // 4. FALLBACK DI SICUREZZA
                // Qualsiasi errore (firma invalida, token scaduto, JSON malformato)
                // si traduce istantaneamente in un blocco senza ulteriori spiegazioni al client
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
        };
    }

    /**
     * Trasforma la stringa segreta configurata nell'application.yml 
     * (tramite AppConfig) in una SecretKey crittografica.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = appConfig.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}