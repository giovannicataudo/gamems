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
    private final RateLimit rateLimit = new RateLimit();


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

    public RateLimit getRateLimit(){
        return rateLimit;
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

    // --- CLASSI ANNIDATE PER IL RATE LIMIT ---
    public static class RateLimit {
        private final Policy anonymous = new Policy();
        private final Policy authenticated = new Policy();

        public Policy getAnonymous() { return anonymous; }
        public Policy getAuthenticated() { return authenticated; }

        /**
         * Struttura condivisa sia per 'anonymous' che per 'authenticated'
         */
        public static class Policy {
            private int requests;
            private int durationSec; // Spring mappa in automatico "duration-sec"

            public int getRequests() { return requests; }
            public void setRequests(int requests) { this.requests = requests; }
            public int getDurationSec() { return durationSec; }
            public void setDurationSec(int durationSec) { this.durationSec = durationSec; }
        }
    }
}