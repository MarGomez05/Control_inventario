package com.tamachihua_erp.api.dto.app;

import java.math.BigDecimal;

// Existencia de una presentación en la API móvil. No incluye stock mínimo.
public record AppStockDto(

        String presentationName,
        BigDecimal stock,
        String stockStatus
) {
}
