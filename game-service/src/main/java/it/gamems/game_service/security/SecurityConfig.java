package it.gamems.game_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ========================================================
 * CONFIGURAZIONE: SecurityConfig
 * ========================================================
 * Definisce la politica di sicurezza per il Game Service.
 * Implementa il paradigma Defense in Depth validando localmente
 * l'identità dell'utente estratta dal JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Configurazione della catena di filtri di sicurezza.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // 1. Disabilitiamo CSRF perché le API sono stateless e usano JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Definizione delle regole di autorizzazione
            .authorizeHttpRequests(auth -> auth
                // Rotte di monitoraggio: accessibili per i check di salute del sistema
                .requestMatchers("/actuator/health").permitAll()
                
                // Rotte di Gioco: richiedono che l'utente sia autenticato (JWT valido)
                .requestMatchers("/api/v1/game/**").authenticated()
                
                // Ogni altra richiesta deve essere autenticata
                .anyRequest().authenticated()
            )
            
            // 3. Gestione della sessione STATELESS
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. Inserimento del filtro JWT prima del filtro di autenticazione standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
}