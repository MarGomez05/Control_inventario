package com.tamachihua_erp.api.dto.presentation;

import com.tamachihua_erp.api.dto.measureunit.MeasureUnitSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Respuesta de la API para una presentación; incluye measureUnit {id, name, code}
public record PresentationResponseDto(

        Long id,
        String name,
        BigDecimal quantity,
        MeasureUnitSummaryDto measureUnit,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
