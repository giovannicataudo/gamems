package it.gamems.game_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * ========================================================
 * CONFIGURAZIONE: RestClientConfig
 * ========================================================
 * Registra esplicitamente il Builder del RestClient nel 
 * contesto di Spring, rendendolo iniettabile in componenti 
 * come il WalletClient.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}