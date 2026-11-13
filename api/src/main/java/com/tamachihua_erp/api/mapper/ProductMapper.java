package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.product.ProductDetailDto;
import com.tamachihua_erp.api.dto.product.ProductListItemDto;
import com.tamachihua_erp.api.dto.product.StockByPresentationDto;
import com.tamachihua_erp.api.dto.category.ProductCategorySummaryDto;
import com.tamachihua_erp.api.dto.model.ProductModelSummaryDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.dto.producttype.ProductTypeSummaryDto;
import com.tamachihua_erp.api.entity.Product;
import com.tamachihua_erp.api.entity.ProductModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductBrandMapper brandMapper;

    // Elemento del listado del panel, con la existencia de cada presentación
    public ProductListItemDto toListItem(
            Product product,
            List<StockByPresentationDto> stockByPresentation
    ) {

        return new ProductListItemDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getProductCategory().getName(),
                product.getProductType().getName(),
                product.getRequiresExpiration(),
                product.getIsActive(),
                stockByPresentation
        );
    }

    // Detalle del producto con sus presentaciones
    public ProductDetailDto toDetailDto(
            Product product,
            List<ProductPresentationResponseDto> presentations
    ) {

        ProductModel model = product.getProductModel();

        ProductModelSummaryDto modelDto = model == null
                ? null
                : new ProductModelSummaryDto(
                        model.getId(),
                        model.getName(),
                        brandMapper.toSummaryDto(model.getProductBrand())
                );

        return new ProductDetailDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                new ProductCategorySummaryDto(
                        product.getProductCategory().getId(),
                        product.getProductCategory().getName()
                ),
                new ProductTypeSummaryDto(
                        product.getProductType().getId(),
                        product.getProductType().getName()
                ),
                modelDto,
                product.getRequiresExpiration(),
                product.getIsActive(),
                presentations
        );
    }
}
