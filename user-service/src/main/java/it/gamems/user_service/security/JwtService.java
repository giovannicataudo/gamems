package it.gamems.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gamems.user_service.config.JwtProperties;
import it.gamems.user_service.entity.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ========================================================
 * SERVICE: JwtService
 * ========================================================
 * Gestisce il ciclo di vita dei token JWT: generazione, 
 * estrazione dati e validazione.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "userId";

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Genera un nuovo token per l'utente appena autenticato.
     * Inseriamo i claim "extra" per far si che l'api gataway
     * potrà far accedere l'uetnte con i suoi privilegi senza
     * passare per il db.
     */
    public String generateToken(User user) {
        // Creazione della HashMap che conterrà tutte le chiamate (claims)
        // che non fanno parte di un JWT standard
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_ROLE, user.getRole().name());
        extraClaims.put(CLAIM_USER_ID, user.getId());
        
        return buildToken(extraClaims, user.getEmail(), jwtProperties.getExpirationMs());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        // Creazione effettiva della stringa JWT unendo le claims personalizzate
        // a quelle standard del payload
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // Creazione della firma usando SHA 256
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                // Builda il toker in formato header.payload.signature in base 64
                .compact();
    }

    /**
     * Estrae l'email (subject) dal token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifica se il token è ancora valido e appartiene all'utente corretto.
     */
    public boolean isTokenValid(String token, String userEmail) {
        final String email = extractEmail(token);
        return (email.equals(userEmail)) && !isTokenExpired(token);
    }

    // Verfica se il token è scaduto
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // Funzione generica per estrarre una singola informazione precisa
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // Chiama il metodo sotto per estrarre il payload dal token valido
        final Claims claims = extractAllClaims(token);
        // Restituisci ciò che ti è stato chiesto
        return claimsResolver.apply(claims);
    }
    // Es per estrarre email: String email = extractClaim(token, Claims::getSubject);

    // Riceve il token e lo "scompatta" in un Claims con tutte le info
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                // Verifica che la firma del token coincida, altrimenti eccezione
                .verifyWith(getSigningKey())
                .build()
                // Verifica che il token non sia stato manomesso e non sia scaduo, pena eccezione
                .parseSignedClaims(token)
                // Prende solo il payload (che contiene le info per conoscere l'user)
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