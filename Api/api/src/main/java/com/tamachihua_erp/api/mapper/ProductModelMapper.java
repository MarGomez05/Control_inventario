package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.model.ProductModelResponseDto;
import com.tamachihua_erp.api.entity.ProductModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// La entidad se arma en el servicio, porque necesita buscar la marca (brandId)
@Component
@RequiredArgsConstructor
public class ProductModelMapper {

    private final ProductBrandMapper brandMapper;

    public ProductModelResponseDto toResponseDto(ProductModel model) {

        return new ProductModelResponseDto(
                model.getId(),
                model.getName(),
                brandMapper.toSummaryDto(model.getProductBrand()),
                model.getIsActive(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }
}
