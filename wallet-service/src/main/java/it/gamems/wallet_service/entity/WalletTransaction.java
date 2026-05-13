package it.gamems.wallet_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ========================================================\n *
 *  ENTITY: WalletTransaction (Libro Mastro)
 *  Traccia in modo immutabile ogni singolo movimento di denaro.
 * ========================================================\n *
 */
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // L'ID della partita generato dal Game Service. È il nostro "Ponte" tra i due db.
    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Column(nullable = false)
    private BigDecimal amount;

    // DEBIT (Scommessa) o REFUND (Rimborso compensativo)
    @Column(nullable = false)
    private String transactionType; 

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Costruttori, Getter e Setter
    public WalletTransaction() {}

    public WalletTransaction(String userId, Long matchId, BigDecimal amount, String transactionType) {
        this.userId = userId;
        this.matchId = matchId;
        this.amount = amount;
        this.transactionType = transactionType;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getMatchId() { return matchId; }
    public BigDecimal getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
}