package it.gamems.game_service.client;

import java.math.BigDecimal;

/**
 * DTO speculare ad AmountRequestDto del Wallet Service per la comunicazione via REST.
 */
public record WalletAmountDto(BigDecimal amount) {}