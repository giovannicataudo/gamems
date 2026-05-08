package it.gamems.wallet_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ========================================================
 * ENTITY: Wallet (Portafoglio Utente)
 * ========================================================
 * Questa classe mappa la tabella "wallets" nel database PostgreSQL
 * assegnato esclusivamente a questo microservizio.
 * * CORE BUSINESS LOGIC:
 * Implementa la separazione dei fondi per il "Playthrough 1x" (AML):
 * - realBalance: I fondi depositati dall'utente (bloccati per il prelievo).
 * - withdrawableBalance: I fondi vinti (liberi per il prelievo).
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    // PK della teballe (id del db locale)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'ID (id reale) dell'utente. Poiché l'autenticazione è delegata all'API Gateway,
     * questo servizio non conosce email o password, riceve solo l'ID utente 
     * univoco dal token JWT. È marcato come 'unique' perché c'è un rapporto 1:1.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /**
     * Saldo Reale (Depositi). 
     * NOTA IMPORTANTE: Nel fintech/gambling si usa SEMPRE BigDecimal.
     * Mai usare Double o Float, in quanto i calcoli in virgola mobile 
     * della CPU possono generare approssimazioni (es. 0.1 + 0.2 = 0.30000000000000004).
     */
    @Column(name = "real_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal realBalance = BigDecimal.ZERO;

    /**
     * Saldo Prelevabile (Vincite).
     */
    @Column(name = "withdrawable_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal withdrawableBalance = BigDecimal.ZERO;

    /**
     * LOCK OTTIMISTICO (@Version).
     * Questo è il punto più critico del sistema distribuito.
     * Se l'utente clicca velocemente due volte "Gioca" con 10€ residui, 
     * si creerebbero due thread concorrenti. Il lock ottimistico fa sì che 
     * il database incrementi questa versione ad ogni salvataggio. Se il secondo 
     * thread prova a salvare usando una versione vecchia, Hibernate lancia 
     * un'eccezione, prevenendo i "doppi prelievi" (Double Spending).
     */
    @Version
    private Long version;
    
    // Campi di auditing per tracciare quando il portafoglio è stato creato/modificato
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Metodi di callback JPA per autocompilare le date
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public BigDecimal getRealBalance() {
        return realBalance;
    }

    public void setRealBalance(BigDecimal realBalance) {
        this.realBalance = realBalance;
    }

    public BigDecimal getWithdrawableBalance() {
        return withdrawableBalance;
    }

    public void setWithdrawableBalance(BigDecimal withdrawableBalance) {
        this.withdrawableBalance = withdrawableBalance;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}