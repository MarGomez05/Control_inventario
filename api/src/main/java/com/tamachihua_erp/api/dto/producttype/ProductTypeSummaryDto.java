package com.tamachihua_erp.api.dto.producttype;

// Versión corta del tipo de producto, para incluirla en la respuesta de un producto: productType {id, name}
public record ProductTypeSummaryDto(

        Long id,
        String name
) {
}
