package it.gamems.game_service.dto;

import java.math.BigDecimal;

/**
 * ========================================================
 * DTO: GamePlayResponseDto (Risultato Lancio)
 * ========================================================
 * Questo DTO viene restituito ESCLUSIVAMENTE come risposta alla 
 * chiamata POST /play. 
 * Contiene i dettagli della singola partita appena conclusa.
 */
public record GamePlayResponseDto(
        
        Long matchId,
        String userChoice,
        String winningSide,
        boolean hasWon,
        BigDecimal betAmount,
        BigDecimal winAmount,
        
        // Campo esclusivo per la singola giocata: un messaggio di feedback per la UX
        String message
) {
}