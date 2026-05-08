package it.gamems.game_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

/**
 * ========================================================
 * CONFIGURAZIONE: AppConfig
 * ========================================================
 * Questa classe mappa le proprietà personalizzate definite 
 * nell'application.yml sotto il prefisso "app".
 * L'uso di @ConfigurationProperties è lo standard nei microservizi
 * per gestire gli URL degli altri servizi in modo tipizzato.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfig {

    private final WalletService walletService = new WalletService();
    private final Internal internal = new Internal();

    public WalletService getWalletService() {
        return walletService;
    }

    public Internal getInternal() {
        return internal;
    }

    /**
     * Classe interna per mappare "app.wallet-service"
     */
    public static class WalletService {
        
        @NotBlank(message = "L'URL del Wallet Service deve essere configurato")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    // Classe interna per mappare app.internal
    public static class Internal {
        @NotBlank(message = "Il segreto interno M2M è obbligatorio")
        private String secret;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
}