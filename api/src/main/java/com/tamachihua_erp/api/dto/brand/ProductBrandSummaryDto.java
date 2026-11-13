package com.tamachihua_erp.api.dto.brand;

// Versión corta de la marca, para incluirla en la respuesta de un modelo: brand {id, name}
public record ProductBrandSummaryDto(

        Long id,
        String name
) {
}
