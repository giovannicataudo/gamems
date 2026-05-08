package it.gamems.game_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ========================================================
 * FILTER: JwtAuthenticationFilter (Lightweight)
 * ========================================================
 * Intercetta ogni richiesta al wallet-service.
 * A differenza dell'Identity Service, NON interroga il database.
 * Si fida ciecamente dei claims (ruolo e ID) estratti dal JWT,
 * a patto che la firma del token sia matematicamente valida.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Estrazione dell'header
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // 2. Validazione crittografica (Se fallisce, salta direttamente nel catch)
            if (jwtService.isTokenValid(jwt)) {
                
                // 3. Estrazione dati bypassando il DB
                String userId = jwtService.extractUserId(jwt);
                String role = jwtService.extractRole(jwt);

                // Spring Security pretende che i ruoli inizino con "ROLE_"
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                // 4. Creazione del passaporto. 
                // NOTA CRITICA: Usiamo userId come "Principal" principale, non l'email.
                // Così nei Controller del Wallet avremo subito l'ID per fare le query.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, 
                        null, 
                        List.of(authority)
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 5. Autenticazione completata
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("Token scaduto per la richiesta a: {}", request.getRequestURI());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Firma JWT non valida (possibile manomissione). IP: {}", request.getRemoteAddr());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Formato JWT non valido. IP: {}", request.getRemoteAddr());
        } catch (Exception e) {
            log.error("Errore critico interno durante la validazione del JWT nel Wallet", e);
        }

        // 6. Si passa la palla al SecurityConfig a prescindere dall'esito
        filterChain.doFilter(request, response);
    }
}