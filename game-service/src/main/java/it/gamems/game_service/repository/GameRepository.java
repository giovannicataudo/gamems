package it.gamems.game_service.repository;

import it.gamems.game_service.entity.Game;
import it.gamems.game_service.enums.GameStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ========================================================
 * REPOSITORY: GameRepository
 * ========================================================
 * Gestisce l'accesso ai dati (Data Access Object) per l'entità Game.
 * Interagisce in modo esclusivo con lo schema 'game_db' su PostgreSQL.
 * Fornisce le operazioni CRUD base in modo automatico.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * CORE BUSINESS QUERY: Recupera lo storico delle partite di un utente.
     * * PERCHÉ È FONDAMENTALE:
     * Permetterà di esporre un endpoint (es. GET /api/v1/game/history) 
     * affinché il frontend possa renderizzare la tabella con gli esiti passati.
     * * COME FUNZIONA:
     * Spring Data JPA traduce linguisticamente il nome del metodo in questa query SQL:
     * SELECT * FROM games WHERE user_id = ? ORDER BY created_at DESC
     * * L'uso di 'OrderByCreatedAtDesc' assicura che il database faccia il lavoro
     * di ordinamento, restituendo la partita più recente come primo elemento della lista,
     * risparmiando cicli di CPU sulla nostra JVM.
     * * @param userId L'identificativo univoco dell'utente (ricevuto dal Gateway)
     * @return Una lista di partite ordinate dalla più recente alla più vecchia.
     */
    List<Game> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Esegue un UPDATE diretto sul database per cambiare lo stato.
     */
    @Modifying
    @Query("UPDATE Game g SET g.status = :status WHERE g.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") GameStatus status);

    /**
     * Sistema di Self-Healing.
     * Aggiorna massivamente lo stato delle partite vecchie bloccate in un determinato stato.
     * @param oldStatus Lo stato in cui la partita è bloccata (es. PENDING)
     * @param newStatus Il nuovo stato da assegnare (es. FAILED)
     * @param threshold La soglia temporale oltre la quale considerare la partita orfana
     * @return Il numero di record aggiornati (utile per i log)
     */
    @Modifying
    @Query("UPDATE Game g SET g.status = :newStatus WHERE g.status = :oldStatus AND g.createdAt <= :threshold")
    int updateStaleGames(
            @Param("oldStatus") GameStatus oldStatus, 
            @Param("newStatus") GameStatus newStatus, 
            @Param("threshold") java.time.LocalDateTime threshold
    );

}