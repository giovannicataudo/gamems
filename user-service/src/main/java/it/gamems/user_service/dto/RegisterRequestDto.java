package it.gamems.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ========================================================
 * DTO: RegisterRequestDto (Input)
 * ========================================================
 * Payload ricevuto al momento della creazione di un nuovo account.
 */
public record RegisterRequestDto(
        
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email,

        @NotBlank(message = "La password è obbligatoria")
        @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
        String password
) {
}