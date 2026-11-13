package com.tamachihua_erp.api.dto.auth;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserAuthResponseDto user
) {}