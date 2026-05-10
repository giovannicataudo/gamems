package it.gamems.user_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ========================================================
 * CONFIGURAZIONE: SecurityConfig
 * ========================================================
 * Il cuore della configurazione di Spring Security.
 * Gestisce l'abilitazione delle rotte, l'algoritmo di hashing 
 * delle password e l'iniezione del filtro JWT.
 * decide chi ha il permesso di entrare, come verificare le password 
 * e quali porte tenere aperte o chiuse.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    // Viete iniettata in automatico CustomUserDetailsService
    private final UserDetailsService userDetailsService;

    // Iniettiamo la chiave segreta definita nel file YAML
    @Value("${app.internal.secret}")
    private String internalSecret;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 1. LA CATENA DI FILTRI (SecurityFilterChain)
     * Definisce le regole di accesso HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disabilitiamo il CSRF perché non usiamo cookie di sessione ma JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // Definiamo i permessi per le singole rotte
            .authorizeHttpRequests(auth -> auth
                // Endpoint pubblici: tutti possono registrarsi o loggarsi
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Endpoint di Actuator: utili per monitorare la salute del servizio
                .requestMatchers("/actuator/health").permitAll()
                // Tutte le altre richieste DEVONO avere un token valido
                // Se l'utente non è nel Security Context scatta il 401
                // ROTTE INTERNE: Controllo della "parola d'ordine" Machine-to-Machine
                .requestMatchers("/api/v1/internal/**").access((authentication, context) -> {
                    // Estraiamo dall'intero http l'header che contiene l'api-key
                    String requestHeaderSecret = context.getRequest().getHeader("X-Internal-Secret");
                    // Confrotniamo le api-key
                    boolean granted = internalSecret.equals(requestHeaderSecret);
                    // Ritorniamo la decisione ("Approvato" se true, "Negato" se false)
                    return new AuthorizationDecision(granted);
                })
                .anyRequest().authenticated()
            )
            
            // Impostiamo la gestione della sessione su STATELESS
            // Ogni richiesta deve essere valutata singolarmente tramite JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Impostiamo il provider che verificherà le credenziali sul database
            .authenticationProvider(authenticationProvider())
            
            // Aggiungiamo il nostro filtro JWT prima di quello standard di Spring Security
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 2. L'AUTENTICATORE (AuthenticationProvider)
     * Collega il nostro UserDetailsService e il PasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Passiamo il userDetailsService DIRETTAMENTE nel costruttore
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        
        // Impostiamo il decifratore di password
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return authProvider;
    }

    /**
     * 3. L'ALGORITMO DI HASHING (PasswordEncoder)
     * Usiamo BCrypt per la protezione delle password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 4. GESTORE AUTENTICAZIONE (AuthenticationManager)
     * Viene utilizzato per validare le credenziali
     * L'AuthenticationManager prenderà la richiesta, la passerà al DaoAuthenticationProvider
     * che a sua volta cercherà l'utente tramite il UserDetailsService e confronterà 
     * l'hash della password con il PasswordEncoder. Se tutto combacia, l'utente viene autenticato.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}