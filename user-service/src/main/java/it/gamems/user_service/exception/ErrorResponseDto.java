package it.gamems.user_service.exception;

import java.time.LocalDateTime;

/**
 * ========================================================
 * DTO: ErrorResponseDto
 * ========================================================
 * Formato standardizzato per tutte le risposte di errore.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}