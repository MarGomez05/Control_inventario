package com.tamachihua_erp.api.dto.product;

import com.tamachihua_erp.api.dto.category.ProductCategorySummaryDto;
import com.tamachihua_erp.api.dto.model.ProductModelSummaryDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.dto.producttype.ProductTypeSummaryDto;

import java.util.List;

// Detalle de un producto con sus presentaciones (ProductDetail del contrato 5.1)
public record ProductDetailDto(

        Long id,
        String code,
        String name,
        String description,
        ProductCategorySummaryDto category,
        ProductTypeSummaryDto productType,
        ProductModelSummaryDto productModel,
        Boolean requiresExpiration,
        Boolean isActive,
        List<ProductPresentationResponseDto> presentations
) {
}
