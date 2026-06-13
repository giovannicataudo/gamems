package it.gamems.user_service.dto;

public record MfaRequiredResponseDto(
        String tempToken,
        String message
) {}
