package com.tamachihua_erp.api.dto.lot;

import com.tamachihua_erp.api.dto.supplier.SupplierSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Lote sin costos, para WAREHOUSE (contrato 7)
public record LotResponseDto(

        Long id,
        String lotNumber,
        LotProductDto product,
        LotPresentationDto presentation,
        SupplierSummaryDto supplier,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate entryDate,
        LocalDate expirationDate,
        Long daysToExpire,
        String expirationSeverity,
        BigDecimal receivedQuantity,
        BigDecimal availableQuantity,
        String notes,
        Boolean isActive
) implements LotView {
}
