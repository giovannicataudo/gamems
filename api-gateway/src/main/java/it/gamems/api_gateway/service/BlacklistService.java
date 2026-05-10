package it.gamems.api_gateway.service;

import it.gamems.api_gateway.client.UserClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Questa classe carica in ram la lista di ids bannati
@Service
public class BlacklistService {
    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);
    
    // Set thread-safe per prestazioni O(1)
    private final Set<Long> bannedUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final UserClient userClient;

    public BlacklistService(UserClient userClient) {
        this.userClient = userClient;
    }

    @PostConstruct
    public void init() {
        log.info("Sincronizzazione iniziale Blacklist...");
        var ids = userClient.fetchBannedUserIds();
        if (ids != null) {
            bannedUsers.addAll(ids);
            log.info("Blacklist caricata in RAM: {} utenti.", bannedUsers.size());
        }
    }

    public void add(Long userId) { bannedUsers.add(userId); }
    public void remove(Long userId) { bannedUsers.remove(userId); }
    public boolean isBanned(Long userId) {
    boolean banned = bannedUsers.contains(userId);
    log.info("Verifica Ban per ID {}: {}", userId, banned);
    return banned;
    }
}