package it.gamems.api_gateway.service;

import it.gamems.api_gateway.client.UserClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Gestore della Blacklist 100% Stateless.
 * I dati risiedono in Redis. Questo permette di avere N repliche
 * dell'API Gateway su Kubernetes perfettamente sincronizzate.
 */
@Service
public class BlacklistService {
    
    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);
    // La chiave identificativa del nostro Set su Redis
    private static final String BLACKLIST_KEY = "security:blacklist:users";

    private final UserClient userClient;
    private final StringRedisTemplate redisTemplate;

    public BlacklistService(UserClient userClient, StringRedisTemplate redisTemplate) {
        this.userClient = userClient;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // OTTIMIZZAZIONE K8S: Se abbiamo scalato a 3 pod, solo il primo 
            // che si avvia popolerà Redis. Gli altri salteranno questo passaggio.
            if (Boolean.FALSE.equals(redisTemplate.hasKey(BLACKLIST_KEY))) {
                log.info("Blacklist assente in Redis. Sincronizzazione iniziale da User Service...");
                var ids = userClient.fetchBannedUserIds();
                
                if (ids != null && !ids.isEmpty()) {
                    // Redis lavora meglio con le stringhe. Convertiamo i Long.
                    String[] idsArray = ids.stream().map(String::valueOf).toArray(String[]::new);
                    // Aggiungiamo tutti gli ID al Set in un solo colpo
                    redisTemplate.opsForSet().add(BLACKLIST_KEY, idsArray);
                    log.info("Blacklist sincronizzata in Redis: {} utenti.", ids.size());
                } else {
                    log.info("Nessun utente bannato trovato durante la sincronizzazione.");
                }
            } else {
                log.info("Blacklist già presente in Redis. Sincronizzazione al boot saltata.");
            }
        // Se l'api non riesce a caricare la lista per problemi di rete
        } catch (Exception e) {
            log.error("Errore durante il recupero iniziale della blacklist: {}. Il sistema farà affidamento sui messaggi RabbitMQ.", e.getMessage());
        }
    }

    public void add(Long userId) { 
        redisTemplate.opsForSet().add(BLACKLIST_KEY, String.valueOf(userId));
        log.info("Utente [{}] AGGIUNTO alla blacklist in Redis.", userId);
    }
    
    public void remove(Long userId) { 
        redisTemplate.opsForSet().remove(BLACKLIST_KEY, String.valueOf(userId));
        log.info("Utente [{}] RIMOSSO dalla blacklist in Redis.", userId);
    }
    
    public boolean isBanned(Long userId) {
        Boolean banned = redisTemplate.opsForSet().isMember(BLACKLIST_KEY, String.valueOf(userId));
        // Evitiamo Null, se per qualche motivo banned è null result sarà false
        boolean result = Boolean.TRUE.equals(banned);
        
        if (result) {
            // Logghiamo solo se è bannato per non intasare i log di sistema
            // per ogni singola richiesta lecita che passa dal Gateway.
            log.warn("Accesso bloccato: l'utente [{}] è presente nella blacklist di Redis.", userId);
        }
        
        return result;
    }
}