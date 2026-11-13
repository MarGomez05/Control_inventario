package com.tamachihua_erp.api.dto.productprice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Elemento del historial de precios de una presentación del producto (contrato 5.3)
public record ProductPriceResponseDto(

        Long id,
        BigDecimal price,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ChangedByDto changedBy
) {
}
