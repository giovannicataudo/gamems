package it.gamems.user_service.dto;

import java.util.List;

// Dto per rispondere a api/v1/admin/users
public record UserListResponseDto(
    List<UserProfileDto> users,
    int totalCount
) {}