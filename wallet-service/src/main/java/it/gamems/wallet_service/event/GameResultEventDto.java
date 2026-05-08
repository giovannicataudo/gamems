package it.gamems.wallet_service.event;

import java.math.BigDecimal;

/**
 * ========================================================
 * EVENT DTO: GameResultEventDto
 * ========================================================
 * Struttura dati immutabile (Record) che mappa il messaggio JSON 
 * proveniente dal Game Service tramite RabbitMQ.
 */
public record GameResultEventDto(

        // id della partita
        long matchId,
        
        // A chi appartiene la partita
        String userId,
        
        // Esito: true se è uscito il lato scelto dall'utente, false altrimenti
        boolean hasWon,
        
        // Importo totale vinto (puntata restituita + profitto).
        // Sarà 0.00 se hasWon è false.
        BigDecimal winAmount
) {
}