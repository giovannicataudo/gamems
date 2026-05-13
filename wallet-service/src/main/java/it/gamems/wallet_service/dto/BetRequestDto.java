package it.gamems.wallet_service.dto;

import java.math.BigDecimal;

public record BetRequestDto(
        BigDecimal amount,
        Long matchId
) {}