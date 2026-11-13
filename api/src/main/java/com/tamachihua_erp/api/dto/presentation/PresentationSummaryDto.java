package com.tamachihua_erp.api.dto.presentation;

import java.math.BigDecimal;

// Versión corta de la presentación para el detalle de un producto:
// presentation {id, name, measureUnit, quantity}
public record PresentationSummaryDto(

        Long id,
        String name,
        String measureUnit,
        BigDecimal quantity
) {
}
