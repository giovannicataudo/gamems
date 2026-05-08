package it.gamems.game_service.service;

import it.gamems.game_service.entity.Game;
import it.gamems.game_service.enums.GameStatus;
import it.gamems.game_service.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ========================================================
 * SERVICE: GamePersistenceService
 * ========================================================
 * Isola le scritture su database dal resto della logica di business.
 * Utilizza Propagation.REQUIRES_NEW per forzare Spring a committare 
 * immediatamente i dati, garantendo la tracciabilità per il Pattern Saga.
 */
@Service
public class GamePersistenceService {

    private final GameRepository gameRepository;

    public GamePersistenceService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Game createPendingGame(Game game) {
        game.setStatus(GameStatus.PENDING);
        // Quando questo metodo termina, la riga è fisicamente su PostgreSQL
        return gameRepository.save(game);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateGameStatus(Long gameId, GameStatus status) {
        gameRepository.updateStatus(gameId, status);
    }
}