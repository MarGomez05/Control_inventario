package com.tamachihua_erp.api.dto.supplier;

// Versión corta del proveedor para la respuesta de un lote: supplier {id, tradeName}
public record SupplierSummaryDto(

        Long id,
        String tradeName
) {
}
