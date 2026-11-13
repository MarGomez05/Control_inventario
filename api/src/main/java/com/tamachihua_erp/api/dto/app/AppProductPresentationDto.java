package com.tamachihua_erp.api.dto.app;

import java.math.BigDecimal;

// Presentación en el detalle de la app: precio vigente, existencia y estado.
// No incluye stock mínimo, costos ni proveedores (HU-14).
public record AppProductPresentationDto(

        String code,
        String name,
        String unit,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal stock,
        String stockStatus
) {
}
