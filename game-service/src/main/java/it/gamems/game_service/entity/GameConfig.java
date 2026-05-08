package it.gamems.game_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Tabella che contiene lo stato del gioco
@Entity
@Table(name = "game_config")
public class GameConfig {

    @Id
    @Column(name = "config_key")
    private String configKey; // Es: "COIN_FLIP_STATUS"

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public GameConfig() {}

    public GameConfig(String configKey, boolean isActive) {
        this.configKey = configKey;
        this.isActive = isActive;
    }

    public String getConfigKey() { return configKey; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}