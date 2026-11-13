package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.productprice.PriceRequestDto;
import com.tamachihua_erp.api.dto.productprice.ProductPriceResponseDto;

import java.util.List;

// Operaciones del historial de precios (HU-08)
public interface ProductPriceService {

    ProductPriceResponseDto changePrice(
            Long productId,
            Long presentationId,
            PriceRequestDto dto,
            Long userId
    );

    List<ProductPriceResponseDto> findHistory(Long productId, Long presentationId);
}
