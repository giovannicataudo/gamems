package it.gamems.game_service.client;

import java.math.BigDecimal;

/**
 * Payload inviato al Wallet Service per richiedere l'addebito di una giocata.
 */
public record WalletBetDto(
        BigDecimal amount,
        Long matchId
) {}