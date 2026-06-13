package it.gamems.user_service.dto;

public record MfaSetupResponseDto(
        String qrCodeUri,
        String message
) {}
