package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.product.ProductDetailDto;
import com.tamachihua_erp.api.dto.product.ProductListItemDto;
import com.tamachihua_erp.api.dto.product.ProductRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Operaciones del módulo de productos (HU-06)
public interface ProductService {

    Page<ProductListItemDto> findAll(
            String search,
            Long categoryId,
            Long productTypeId,
            Boolean isActive,
            Pageable pageable
    );

    ProductDetailDto findById(Long id);

    ProductDetailDto create(ProductRequestDto dto);

    ProductDetailDto update(Long id, ProductRequestDto dto);

    // Activar o desactivar (borrado lógico)
    void changeStatus(Long id, Boolean isActive);
}
