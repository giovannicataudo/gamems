package it.gamems.game_service.worker;

import it.gamems.game_service.enums.GameStatus;
import it.gamems.game_service.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    public GamePendingCleanupWorker(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
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

        // Eseguiamo l'aggiornamento massivo direttamente sul database
        int updatedRows = gameRepository.updateStaleGames(GameStatus.PENDING, GameStatus.FAILED, threshold);

        // Log per tenere traccia delle eventuali modifiche
        if (updatedRows > 0) {
            log.warn("SELF-HEALING TRIGGERED: Trovate e chiuse {} partite rimaste bloccate in stato PENDING da prima del {}.", 
                    updatedRows, threshold);
        } else {
            // Log a livello trace per non inquinare la console, utile solo in debug
            log.trace("Self-healing check completato: nessuna partita orfana trovata.");
        }
    }
}