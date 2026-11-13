package com.tamachihua_erp.api.dto.measureunit;

import java.time.LocalDateTime;

// Respuesta de la API para una unidad de medida
public record MeasureUnitResponseDto(

        Long id,
        String name,
        String code,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
