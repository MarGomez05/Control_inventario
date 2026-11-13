package com.tamachihua_erp.api.dto.product;

import java.math.BigDecimal;

// Existencia de una presentación dentro del listado de productos
public record StockByPresentationDto(

        Long productPresentationId,
        String presentationName,
        BigDecimal stock,
        String stockStatus
) {
}
