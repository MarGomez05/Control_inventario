package com.tamachihua_erp.api.dto.lot;

import java.math.BigDecimal;
import java.time.LocalDate;

// Último costo pagado por presentación (contrato 7, GET /api/products/{id}/last-cost)
public record LastCostItemDto(

        Long productPresentationId,
        String presentationName,
        BigDecimal lastUnitCost,
        Long lotId,
        LocalDate entryDate
) {
}
