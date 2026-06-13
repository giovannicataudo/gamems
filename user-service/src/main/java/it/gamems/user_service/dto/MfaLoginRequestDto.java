package it.gamems.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaLoginRequestDto(
        @NotBlank(message = "Il token temporaneo è obbligatorio")
        String tempToken,
        @NotBlank(message = "Il codice MFA è obbligatorio")
        String code
) {}
