package com.tamachihua_erp.api.dto.dashboard;

import java.math.BigDecimal;

// Fila de la tabla de bajo stock (HU-12, contrato 8)
public record LowStockItemDto(

        Long productId,
        String productCode,
        String productName,
        String presentationName,
        BigDecimal currentStock,
        BigDecimal minStock,
        BigDecimal missing
) {
}
