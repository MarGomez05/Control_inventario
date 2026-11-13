package com.tamachihua_erp.api.dto.app;

import java.util.List;

// Detalle de un producto para la app móvil (AppProductDetail del contrato 10)
public record AppProductDetailDto(

        Long id,
        String code,
        String name,
        String description,
        String categoryName,
        String productTypeName,
        String brandName,
        List<AppProductPresentationDto> presentations
) {
}
