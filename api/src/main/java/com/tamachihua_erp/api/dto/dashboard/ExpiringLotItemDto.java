package com.tamachihua_erp.api.dto.dashboard;

import java.time.LocalDate;

// Fila de la tabla de lotes vencidos y por vencer (HU-12, contrato 8)
public record ExpiringLotItemDto(

        Long lotId,
        String productName,
        String presentationName,
        String lotNumber,
        LocalDate expirationDate,
        long daysRemaining,
        String severity,
        String label
) {
}
