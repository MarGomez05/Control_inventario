package com.tamachihua_erp.api.dto.model;

import com.tamachihua_erp.api.dto.brand.ProductBrandSummaryDto;

import java.time.LocalDateTime;

// Respuesta de la API para un modelo; incluye brand {id, name} (contrato 4.4)
public record ProductModelResponseDto(

        Long id,
        String name,
        ProductBrandSummaryDto brand,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
