package it.gamems.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * ========================================================
 * DTO: LoginRequestDto (Input)
 * ========================================================
 * Payload ricevuto quando l'utente tenta di accedere.
 */
public record LoginRequestDto(
        
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email,

        @NotBlank(message = "La password è obbligatoria")
        String password
) {
}