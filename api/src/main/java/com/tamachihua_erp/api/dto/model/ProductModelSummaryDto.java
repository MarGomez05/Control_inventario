package com.tamachihua_erp.api.dto.model;

import com.tamachihua_erp.api.dto.brand.ProductBrandSummaryDto;

// Versión corta del modelo para el detalle del producto: productModel {id, name, brand {id, name}}
public record ProductModelSummaryDto(

        Long id,
        String name,
        ProductBrandSummaryDto brand
) {
}
