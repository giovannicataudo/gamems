package it.gamems.game_service.dto;

import jakarta.validation.constraints.NotNull;

// DTO per settare lo stato (admin)
public record GameStatusRequestDto(
    @NotNull(message = "Lo stato 'active' è obbligatorio")
    Boolean active
) {}