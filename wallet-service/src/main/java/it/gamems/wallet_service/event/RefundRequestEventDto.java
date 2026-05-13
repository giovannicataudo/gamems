package it.gamems.wallet_service.event;

import java.math.BigDecimal;

public record RefundRequestEventDto(
        Long matchId,
        String userId,
        BigDecimal amount
) {}