package com.tamachihua_erp.api.dto.brand;

import java.time.LocalDateTime;

// Respuesta de la API para una marca
public record ProductBrandResponseDto(

        Long id,
        String name,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
