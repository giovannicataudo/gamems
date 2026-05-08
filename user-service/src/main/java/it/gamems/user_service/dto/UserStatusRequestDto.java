package it.gamems.user_service.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO per l'aggiornamento dello stato (abilitato/disabilitato) di un utente.
 */
public record UserStatusRequestDto(
    @NotNull(message = "Lo stato 'enabled' è obbligatorio")
    Boolean enabled
) {}