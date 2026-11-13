package com.tamachihua_erp.api.dto.category;

import java.time.LocalDateTime;

// Este DTO definirá cómo responderá nuestra API cuando consulte una categoría
public record ProductCategoryResponseDto(

        Long id,
        String name,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
