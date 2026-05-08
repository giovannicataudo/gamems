package it.gamems.game_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Questa classe configura e mappa la chiave dei JWT in application.yml
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}