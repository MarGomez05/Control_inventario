package com.tamachihua_erp.api.dto.product;

import java.util.List;

// Elemento del listado paginado de productos (ProductListItem del contrato 5.1)
public record ProductListItemDto(

        Long id,
        String code,
        String name,
        String categoryName,
        String productTypeName,
        Boolean requiresExpiration,
        Boolean isActive,
        List<StockByPresentationDto> stockByPresentation
) {
}
