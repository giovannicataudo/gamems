package it.gamems.game_service.dto;

// DTO per lo stato (active) del gioco in uscita
public record GameStatusResponseDto(
    Boolean active
) {}