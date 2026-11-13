package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationRequestDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationUpdateRequestDto;

import java.util.List;

// Operaciones de las presentaciones de un producto (HU-07)
public interface ProductPresentationService {

    List<ProductPresentationResponseDto> findByProductId(Long productId);

    ProductPresentationResponseDto create(Long productId, ProductPresentationRequestDto dto);

    ProductPresentationResponseDto update(
            Long productId,
            Long id,
            ProductPresentationUpdateRequestDto dto
    );

    void changeStatus(Long productId, Long id, Boolean isActive);
}
