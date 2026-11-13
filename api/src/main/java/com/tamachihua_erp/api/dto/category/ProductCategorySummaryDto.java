package com.tamachihua_erp.api.dto.category;

// Versión corta de la categoría, para incluirla en la respuesta de un producto: category {id, name}
public record ProductCategorySummaryDto(

        Long id,
        String name
) {
}
