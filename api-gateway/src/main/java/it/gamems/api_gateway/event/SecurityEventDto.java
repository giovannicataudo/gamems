package it.gamems.api_gateway.event;

public record SecurityEventDto(
        Long userId,
        String action // Conterrà "BAN" o "UNBAN"
) {
}