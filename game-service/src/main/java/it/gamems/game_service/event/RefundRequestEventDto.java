package it.gamems.game_service.event;

import java.math.BigDecimal;

/**
 * ========================================================
 * EVENT DTO: RefundRequestEvent (Compensazione)
 * ========================================================
 * Payload inviato asincronamente al Wallet Service quando una 
 * partita fallisce per cause di rete dopo il potenziale addebito.
 */
public record RefundRequestEventDto(
        Long matchId,
        String userId,
        BigDecimal amount
) {}