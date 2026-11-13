package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.brand.ProductBrandRequestDto;
import com.tamachihua_erp.api.dto.brand.ProductBrandResponseDto;
import com.tamachihua_erp.api.dto.brand.ProductBrandSummaryDto;
import com.tamachihua_erp.api.entity.ProductBrand;
import org.springframework.stereotype.Component;

@Component
public class ProductBrandMapper {

    public ProductBrand toEntity(ProductBrandRequestDto dto) {

        ProductBrand brand = new ProductBrand();

        brand.setName(dto.name().trim());

        return brand;
    }

    public ProductBrandResponseDto toResponseDto(ProductBrand brand) {

        return new ProductBrandResponseDto(
                brand.getId(),
                brand.getName(),
                brand.getIsActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }

    // Versión corta para incluirla en la respuesta de un modelo
    public ProductBrandSummaryDto toSummaryDto(ProductBrand brand) {

        return new ProductBrandSummaryDto(brand.getId(), brand.getName());
    }
}
