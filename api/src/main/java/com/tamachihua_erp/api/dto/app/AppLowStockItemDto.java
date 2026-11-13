package com.tamachihua_erp.api.dto.app;

import java.math.BigDecimal;

// Producto en bajo stock para la app (HU-16, contrato 10). Sin minStock.
public record AppLowStockItemDto(

        Long productId,
        String productName,
        String presentationName,
        BigDecimal stock,
        String stockStatus
) {
}
