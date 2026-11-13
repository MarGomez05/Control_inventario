package com.tamachihua_erp.api.dto.dashboard;

// Indicadores del dashboard (HU-12, contrato 8)
public record DashboardSummaryDto(

        long activeProducts,
        long lowStockProducts,
        long activeLots,
        long expiringLots,
        long expiredLots
) {
}
