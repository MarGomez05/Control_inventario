package com.tamachihua_erp.api.dto.producttype;

import java.time.LocalDateTime;

// Respuesta de la API para un tipo de producto
public record ProductTypeResponseDto(

        Long id,
        String name,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
