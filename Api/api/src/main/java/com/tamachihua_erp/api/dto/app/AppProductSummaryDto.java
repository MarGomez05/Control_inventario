package com.tamachihua_erp.api.dto.app;

import java.util.List;

// Producto del listado de la app móvil (AppProductSummary del contrato 10)
public record AppProductSummaryDto(

        Long id,
        String code,
        String name,
        String categoryName,
        List<AppStockDto> stock
) {
}
