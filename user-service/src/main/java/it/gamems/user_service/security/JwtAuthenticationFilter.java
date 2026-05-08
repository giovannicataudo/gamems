package it.gamems.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ========================================================
 * FILTER: JwtAuthenticationFilter
 * ========================================================
 * Estende OncePerRequestFilter per garantire che venga 
 * eseguito una e una sola volta per ogni singola richiesta HTTP.
 * Ogni richiesta HTTP passa attraverso questo filtro.
 * Il compito di questa classe non è bloccare, ma mettere gli utenti
 * validati nel Security Context Holder (contesto di sicurezza globale)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Estrazione dell'header Authorization
        final String authHeader = request.getHeader("Authorization");

        final String jwt;
        final String userEmail;

        // Se l'header manca o non ha il formato corretto non blocca perchè
        // potrebbe essere una chiamata ad una pagina publica (login/registrazione).
        // In caso contrario verrà bloccato da Spring più avanti.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Estrazione del token fisico (saltando i primi 7 caratteri: "Bearer ")
        jwt = authHeader.substring(7);
        
        try {
            // Estrazione della mail tramite jwtservice
            userEmail = jwtService.extractEmail(jwt);
            
            // Se abbiamo un'email nel token e l'utente non è ancora nel SecurityContext
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Carichiamo l'utente dal database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Validiamo il token
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    
                    // Creiamo il "lasciapassare" per Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Non passiamo le credenziali (password) per sicurezza
                            userDetails.getAuthorities() // Passiamo i ruoli (es. ROLE_USER)
                    );
                    
                    // Aggiungiamo i dettagli della richiesta (IP, SessionId se presente)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Inseriamo l'utente nel contesto di sicurezza globale
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 1. IL TOKEN È SCADUTO: È una cosa normale. 
            // Logghiamo a livello "DEBUG" o "TRACE" così non intasiamo i log, 
            // e lasciamo che la richiesta prosegua verso il muro del 401.
            logger.debug("Token scaduto per l'utente.");
            
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 2. TOKEN MANOMESSO: Questa non è una cosa normale. Qualcuno sta "hackerando".
            // Logghiamo a livello "WARN" (Attenzione!). 
            // Lasciamo comunque proseguire verso il 401.
            logger.warn("Tentativo di accesso con firma JWT non valida! IP: "+ request.getRemoteAddr());
            
        } catch (Exception e) {
            // 3. ERRORE DI SISTEMA VERO E PROPRIO (es. NullPointerException o Database giù)
            // Questo NON deve essere silenzioso. Lo logghiamo come "ERROR" con tutto lo stacktrace.
            logger.error("Errore critico interno durante la validazione del JWT", e);
        }

        // La richiesta viene sempre lasciata passare.
        // Questa classe si occupa di autenticazione non di autorizzazione
        filterChain.doFilter(request, response);
    }
}