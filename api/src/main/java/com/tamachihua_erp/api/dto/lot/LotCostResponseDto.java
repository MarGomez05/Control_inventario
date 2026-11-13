package com.tamachihua_erp.api.dto.lot;

import com.tamachihua_erp.api.dto.supplier.SupplierSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Lote con costos, para ADMIN y PURCHASING (contrato 7)
public record LotCostResponseDto(

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
        Boolean isActive,
        BigDecimal unitCost,
        BigDecimal totalCost
) implements LotView {
}
