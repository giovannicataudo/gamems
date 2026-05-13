package it.gamems.game_service.worker;

import it.gamems.game_service.entity.Game;
import it.gamems.game_service.enums.GameStatus;
import it.gamems.game_service.event.GameEventPublisher;
import it.gamems.game_service.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ========================================================
 * WORKER: GamePendingCleanupWorker
 * ========================================================
 * Processo in background (Daemon) che si occupa dell'auto-riparazione
 * dei dati. Cerca transazioni rimaste orfane a causa di crash del server
 * e ne chiude il ciclo di vita.
 * L'annotazione @EnableScheduling attiva il motore dei task di Spring.
 */
@Component
@EnableScheduling
public class GamePendingCleanupWorker {

    private static final Logger log = LoggerFactory.getLogger(GamePendingCleanupWorker.class);
    
    private final GameRepository gameRepository;
    private final GameEventPublisher eventPublisher;

    public GamePendingCleanupWorker(GameRepository gameRepository,
                                    GameEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Esegue il task ogni 60.000 millisecondi (1 minuto).
     * Cerca le partite PENDING più vecchie di 5 minuti e le marca come FAILED.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupStalePendingGames() {
        // Calcoliamo la soglia di tolleranza: 5 minuti fa esatti
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        // 1. Recuperiamo materialmente le partite bloccate
        List<Game> staleGames = gameRepository.findByStatusAndCreatedAtBefore(GameStatus.PENDING, threshold);

        if (staleGames.isEmpty()) {
            log.trace("Self-healing check completato: nessuna partita orfana trovata.");
            return;
        }

        // 2. Le iteriamo una per una per garantire la Saga
        for (Game game : staleGames) {
            game.setStatus(GameStatus.FAILED);
            gameRepository.save(game);
            
            log.warn("SELF-HEALING: Partita orfana #{} passata in FAILED. Emetto evento di compensazione.", game.getId());
            
            // 3. FONDAMENTALE: Attiviamo la compensazione per recuperare i soldi dal Wallet
            eventPublisher.publishRefundRequest(game.getId(), game.getUserId(), game.getBetAmount());
        }
    }
}