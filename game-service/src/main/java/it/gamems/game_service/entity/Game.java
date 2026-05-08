package it.gamems.game_service.entity;

import it.gamems.game_service.enums.GameStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ========================================================
 * ENTITY: Game (Storico Partite)
 * ========================================================
 * Questa classe mappa la tabella "games" nel database PostgreSQL 
 * assegnato esclusivamente a questo microservizio (game_db).
 * * CORE BUSINESS LOGIC:
 * Registra l'esito immutabile di ogni lancio della moneta.
 * Trattandosi di un registro storico (log append-only), 
 * non utilizziamo il lock ottimistico (@Version) in quanto 
 * questi record non subiranno mai aggiornamenti concorrenti.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Riferimento all'utente (ricevuto dall'API Gateway)
    @Column(name = "user_id", nullable = false)
    private String userId;

    // L'importo scommesso nella partita
    @Column(name = "bet_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal betAmount;

    // La scelta effettuata dal giocatore (es. "TESTA" o "CROCE")
    @Column(name = "user_choice", nullable = false, length = 10)
    private String userChoice;

    // Il risultato fisico del lancio calcolato dal motore (es. "CROCE")
    @Column(name = "winning_side", nullable = false, length = 10)
    private String winningSide;

    // Flag di riepilogo. Fondamentale per query veloci (es. win-rate dell'utente)
    @Column(name = "has_won", nullable = false)
    private boolean hasWon;

    // Importo totale vinto (puntata restituita + profitto). 
    // Sarà esattamente 0.00 se hasWon è false.
    @Column(name = "win_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal winAmount;

    // Data e ora esatta in cui è avvenuto il lancio
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Stato della partita
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status;

    // Callback JPA per impostare automaticamente il timestamp prima dell'inserimento a DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ========================================================
    // GETTER & SETTER STANDARD
    // ========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    public String getUserChoice() {
        return userChoice;
    }

    public void setUserChoice(String userChoice) {
        this.userChoice = userChoice;
    }

    public String getWinningSide() {
        return winningSide;
    }

    public void setWinningSide(String winningSide) {
        this.winningSide = winningSide;
    }

    public boolean isHasWon() {
        return hasWon;
    }

    public void setHasWon(boolean hasWon) {
        this.hasWon = hasWon;
    }

    public BigDecimal getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = winAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}