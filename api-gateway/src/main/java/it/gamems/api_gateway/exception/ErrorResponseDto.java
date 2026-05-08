package it.gamems.api_gateway.exception;

import java.time.LocalDateTime;

/**
 * ========================================================
 * DTO: ErrorResponseDto (Standard Ecosistema)
 * ========================================================
 * Struttura fissa per tutti gli errori ritornati dal sistema.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}