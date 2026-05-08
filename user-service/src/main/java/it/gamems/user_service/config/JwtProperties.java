package it.gamems.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * Mappa le proprietà sotto 'app.jwt' dall'application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "La chiave segreta JWT non può essere vuota")
    private String secret;

    @NotNull(message = "La scadenza JWT deve essere specificata")
    private Long expirationMs;

    // Getter e Setter
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(Long expirationMs) { this.expirationMs = expirationMs; }
}