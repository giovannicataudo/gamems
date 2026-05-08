package it.gamems.user_service.dto;

import java.time.LocalDateTime;

/**
 * ========================================================
 * DTO: UserProfileDto (Output)
 * ========================================================
 * Rappresenta i dati visibili del profilo utente.
 * ESCLUDE categoricamente la password e altri dati sensibili di sistema.
 */
public record UserProfileDto(
        
        Long id,
        String email,
        String role,
        LocalDateTime createdAt
) {
}