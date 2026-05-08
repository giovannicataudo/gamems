package it.gamems.game_service.exception;

import java.time.LocalDateTime;

/**
 * Identico a quello del Wallet, per mantenere lo standard verso il frontend.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}