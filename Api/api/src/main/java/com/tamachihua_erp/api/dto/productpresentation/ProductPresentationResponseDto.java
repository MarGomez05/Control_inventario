package com.tamachihua_erp.api.dto.productpresentation;

import com.tamachihua_erp.api.dto.presentation.PresentationSummaryDto;

import java.math.BigDecimal;

// Presentación del producto con su existencia, mínimo y precio vigente (contrato 5.1 y 5.2)
public record ProductPresentationResponseDto(

        Long id,
        String code,
        PresentationSummaryDto presentation,
        BigDecimal minStock,
        BigDecimal currentPrice,
        BigDecimal stock,
        String stockStatus,
        Boolean isActive
) {
}
