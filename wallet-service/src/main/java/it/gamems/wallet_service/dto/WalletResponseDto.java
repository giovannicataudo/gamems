package it.gamems.wallet_service.dto;

import java.math.BigDecimal;

/**
 * ========================================================
 * DTO: WalletResponseDto (Dati in uscita)
 * ========================================================
 * Questo Record definisce esattamente cosa il frontend riceverà 
 * quando interroga il saldo dell'utente.
 * * * SICUREZZA E DESIGN:
 * Non esponiamo mai l'Entity Wallet direttamente per due motivi:
 * 1. Non vogliamo mostrare all'esterno l'ID del database o la "@Version".
 * 2. Possiamo calcolare campi "virtuali" (come il totalBalance) che 
 * non esistono nel database, semplificando la vita al frontend.
 */
public record WalletResponseDto(
        
        // Saldo originato dai depositi (vincolato al Playthrough 1x)
        BigDecimal realBalance,
        
        // Saldo originato dalle vincite (prelevabile liberamente)
        BigDecimal withdrawableBalance,
        
        // CAMPO VIRTUALE: La somma dei due saldi. 
        // Utile per la UX del giocatore che vuole vedere il suo potere d'acquisto totale.
        BigDecimal totalBalance
) {
    /**
     * Costruttore compatto del Record. 
     * Viene eseguito automaticamente alla creazione dell'oggetto.
     * Lo usiamo per assicurarci che i valori nulli vengano convertiti in ZERO
     * per evitare NullPointerException nel frontend.
     */
    public WalletResponseDto {
        if (realBalance == null) realBalance = BigDecimal.ZERO;
        if (withdrawableBalance == null) withdrawableBalance = BigDecimal.ZERO;
        if (totalBalance == null) totalBalance = realBalance.add(withdrawableBalance);
    }
}