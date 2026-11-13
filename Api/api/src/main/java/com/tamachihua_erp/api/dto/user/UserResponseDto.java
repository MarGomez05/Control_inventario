package com.tamachihua_erp.api.dto.user;

import com.tamachihua_erp.api.dto.role.RoleSummaryDto;

import java.time.LocalDateTime;

// Respuesta de un usuario del panel; nunca incluye la contraseña (contrato 3)
public record UserResponseDto(

        Long id,
        String username,
        String fullName,
        String email,
        RoleSummaryDto role,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
