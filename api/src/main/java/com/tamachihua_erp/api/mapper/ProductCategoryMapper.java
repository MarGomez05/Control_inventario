package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.category.ProductCategoryRequestDto;
import com.tamachihua_erp.api.dto.category.ProductCategoryResponseDto;
import com.tamachihua_erp.api.entity.ProductCategory;
import org.springframework.stereotype.Component;

// Esta anotación le indica a Spring que debe registrar esta clase como un componente administrado por su contenedor
@Component
public class ProductCategoryMapper {

    // Convierte un Request DTO en una entidad
    public ProductCategory toEntity(ProductCategoryRequestDto dto) {

        ProductCategory category = new ProductCategory();

        category.setName(dto.name());
        category.setDescription(dto.description());

        return category;
    }

    // Convierte una entidad en un Response DTO
    public ProductCategoryResponseDto toResponseDto(ProductCategory category) {

        return new ProductCategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
