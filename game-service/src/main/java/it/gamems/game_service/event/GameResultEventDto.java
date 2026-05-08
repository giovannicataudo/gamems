package it.gamems.game_service.event;

import java.math.BigDecimal;

/**
 * ========================================================
 * EVENT DTO: GameResultEventDto
 * ========================================================
 * Rappresenta il messaggio che il Game Service invierà al 
 * Wallet Service al termine di ogni partita.
 * La struttura deve essere identica a quella che il Wallet si aspetta.
 */
public record GameResultEventDto(
        Long matchId,
        String userId,
        boolean hasWon,
        BigDecimal winAmount
) {
}