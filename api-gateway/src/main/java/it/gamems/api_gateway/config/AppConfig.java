package it.gamems.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;

// Classe che mappa app
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String apiKey;
    private final Jwt jwt = new Jwt();
    private final UserService userService = new UserService();
    private final Internal internal = new Internal();


    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public UserService getUserService(){
        return userService;
    }

    public Internal getInternal() {
        return internal;
    }

    public static class Jwt {
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    /**
     * Classe interna per mappare "app.user-service"
     */
    public static class UserService {
        
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