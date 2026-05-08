package it.gamems.wallet_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ========================================================
 * CONFIGURAZIONE: SecurityConfig (Lightweight)
 * ========================================================
 * Definisce i criteri di accesso per il Wallet Service.
 * Essendo un microservizio a valle, non gestisce il login,
 * ma si limita a validare i permessi dichiarati nel JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Iniettiamo la chiave segreta definita nel file YAML
    @Value("${app.internal.secret}")
    private String internalSecret;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Configurazione della catena di filtri di sicurezza.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // 1. Disabilitiamo CSRF poiché utilizziamo JWT e non sessioni basate su cookie
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configurazione delle regole di autorizzazione sulle rotte
            .authorizeHttpRequests(auth -> auth
                // Rotte ADMIN: Richiedono esplicitamente il ruolo ADMIN estratto dal JWT
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // ROTTE INTERNE: Controllo della "parola d'ordine" Machine-to-Machine
                .requestMatchers("/api/v1/internal/**").access((authentication, context) -> {
                    // Estraiamo dall'intero http l'header che contiene l'api-key
                    String requestHeaderSecret = context.getRequest().getHeader("X-Internal-Secret");
                    // Confrotniamo le api-key
                    boolean granted = internalSecret.equals(requestHeaderSecret);
                    // Ritorniamo la decisione ("Approvato" se true, "Negato" se false)
                    return new AuthorizationDecision(granted);
                })
                
                // Rotte Actuator/Health: Sempre accessibili per il monitoraggio di Docker/K8s
                .requestMatchers("/actuator/health").permitAll()
                
                // Ogni altra richiesta richiede che l'utente sia almeno autenticato (JWT valido)
                .anyRequest().authenticated()
            )
            
            // 3. Gestione della sessione STATELESS (ogni volta si riverifica il token)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. Iniettiamo il nostro filtro JWT personalizzato prima del filtro di autenticazione standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
}