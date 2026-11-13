package com.tamachihua_erp.api.dto.auth;

public record UserAuthResponseDto(
        Long id,
        String username,
        String fullName,
        String role
) {}
