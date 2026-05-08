package it.gamems.game_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ========================================================
 * DTO: GameHistoryItemDto (Elemento Lista)
 * ========================================================
 * Questo DTO viene utilizzato ESCLUSIVAMENTE per la chiamata 
 * GET /history. Rappresenta una singola riga della tabella 
 * dello storico. Non include messaggi testuali per mantenere 
 * il payload JSON il più leggero possibile.
 */
public record GameHistoryItemDto(
        
        Long matchId,
        String userChoice,
        String winningSide,
        boolean hasWon,
        BigDecimal betAmount,
        BigDecimal winAmount,
        
        // Nello storico il timestamp è fondamentale per ordinare la UI
        LocalDateTime playedAt
) {
}