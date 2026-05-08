package it.gamems.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Classe che mappa app
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String apiKey;
    private final Jwt jwt = new Jwt();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Jwt getJwt() {
        return jwt;
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
}