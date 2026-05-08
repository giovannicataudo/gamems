package it.gamems.wallet_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

/**
 * Mappa le proprietà personalizzate definite nell'application.yml 
 * sotto il prefisso "app".
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfig {

    private final Internal internal = new Internal();

    public Internal getInternal() {
        return internal;
    }

    /**
     * Sottoclasse per mappare "app.internal"
     */
    public static class Internal {
        
        @NotBlank(message = "Il segreto interno M2M è obbligatorio per la sicurezza")
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}