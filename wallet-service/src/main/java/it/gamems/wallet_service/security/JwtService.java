package it.gamems.wallet_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gamems.wallet_service.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * ========================================================
 * SERVICE: JwtService
 * ========================================================
 * Gestisce la sola validazione ed estrazione dati dai JWT.
 * Si occupa solo di verificare l'autenticità della firma e leggere i claim.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Estrae l'ID utente personalizzato che avevamo inserito nel token.
     * Usiamo toString() per garantire flessibilità se il claim è salvato come numero o stringa.
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId").toString());
    }

    /**
     * Estrae il ruolo dell'utente (es. "USER", "ADMIN") per i controlli di autorizzazione.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Verifica l'autenticità del token.
     * Se il token è stato manomesso, è scaduto (il parser lo capisce da solo), 
     * o la firma non corrisponde, lancia un'eccezione che catturiamo ritornando false.
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Funzione generica per estrarre una singola informazione (claim)
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Riceve il token e lo "scompatta" in un oggetto Claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                // Verifica che la firma del token coincida con il nostro segreto
                .verifyWith(getSigningKey())
                .build()
                // Verifica che il token non sia stato manomesso e non sia scaduto
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Trasforma la stringa segreta configurata in una SecretKey crittografica.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}