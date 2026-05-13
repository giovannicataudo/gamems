package it.gamems.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import it.gamems.api_gateway.config.AppConfig;
import it.gamems.api_gateway.service.BlacklistService;

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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);


    private final AppConfig appConfig;
    private final BlacklistService blacklistService;

    public JwtAuthFilter(AppConfig appConfig, BlacklistService blacklistService) {
        this.appConfig = appConfig;
        this.blacklistService = blacklistService;
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

                // Conversione del dato per efficacia e coerenza
                Long userIdLong = Long.valueOf(userId);
                // Se l'ID è presente nella nostra lista in memoria, blocchiamo 
                // la richiesta prima che raggiunga i microservizi.
                if (blacklistService.isBanned(userIdLong)) {
                    // Restituiamo 403 FORBIDDEN perchè il token è ancora valido
                    // ma l'utente è bannato
                    return ServerResponse.status(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of(
                                "code", "USER_BANNED",
                                "message", "Il tuo account è stato sospeso. Contatta l'assistenza per ulteriori informazioni."
                            ));
                }

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

            } catch (ExpiredJwtException e) {
                // CASO 1: SCADENZA FISIOLOGICA
                // Il token è valido ma scaduto. Avvisiamo il frontend in modo pulito.
                log.debug("Token scaduto per la richiesta: {}", request.uri());
                return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Token-Expired", "true") // Iniettiamo l'header
                        .build();
                        
            } catch (SignatureException | MalformedJwtException e) {
                // CASO 2: TENTATIVO DI MANOMISSIONE
                // Qualcuno ha alterato il payload o la firma. Possibile attacco.
                log.warn("🚨 ALLARME SICUREZZA: Tentativo di manomissione del token rilevato da IP o per l'URI: {}", request.uri());
                return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Token-Invalid", "true")
                        .build();
                        
            } catch (Exception e) {
                // CASO 3: ERRORE GENERICO E IMPREVISTO
                log.error("Errore imprevisto durante la validazione del token", e);
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