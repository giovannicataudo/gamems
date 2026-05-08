package it.gamems.wallet_service.exception;

import java.time.LocalDateTime;

/**
 * ========================================================
 * DTO: ErrorResponseDto
 * ========================================================
 * Record leggero (Java 25) che definisce la struttura fissa 
 * per tutti i messaggi di errore restituiti dal microservizio.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}