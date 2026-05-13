package it.gamems.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * ========================================================
 * CONFIGURAZIONE: CorsConfig (Cross-Origin Resource Sharing)
 * ========================================================
 * * Senza questa classe il browser bloccherebbe le chiamate 
 * verso l'api_gateway perchè gli risulterebbe una origin
 * diversa rispetto al frontend.
 * L'API Gateway fa da scudo. Centralizzando qui il CORS, 
 * evitiamo di doverlo configurare in ogni singolo microservizio.
 * Garantisce che solo i frontend autorizzati possano comunicare 
 * con il nostro sistema.
 */
@Configuration
public class CorsConfig {

    private final AppConfig appConfig;

    public CorsConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 1. ORIGINI CONSENTITE (Chi può chiamarci?)
       config.setAllowedOrigins(appConfig.getCors().getAllowedOrigins());
        
        // 2. METODI CONSENTITI (Cosa possono fare?)
        // Abilitiamo i verbi HTTP necessari per le nostre API (CRUD e OPTIONS per il pre-flight del browser).
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 3. HEADERS CONSENTITI
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Api-Key"));
        
        // Esponiamo l'header Authorization nel caso il frontend abbia bisogno di leggerlo dalle risposte.
        config.addExposedHeader("Authorization");
        
        // 4. CREDENZIALI
        // Necessario per policy di sicurezza avanzate nei browser moderni quando si scambiano token di autenticazione.
        config.setAllowCredentials(true);

        // 5. APPLICAZIONE GLOBALE
        // Applichiamo queste regole a TUTTE le rotte ("/**") che transitano attraverso il Gateway.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}