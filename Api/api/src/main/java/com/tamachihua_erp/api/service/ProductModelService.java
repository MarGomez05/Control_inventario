package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.model.ProductModelRequestDto;
import com.tamachihua_erp.api.dto.model.ProductModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de modelos (HU-04)
public interface ProductModelService {

    Page<ProductModelResponseDto> findAll(String search, Boolean isActive, Long brandId, Pageable pageable);

    // brandId opcional: si viene, solo los modelos de esa marca
    List<ProductModelResponseDto> findActiveOptions(Long brandId);

    ProductModelResponseDto findById(Long id);

    ProductModelResponseDto create(ProductModelRequestDto dto);

    ProductModelResponseDto update(Long id, ProductModelRequestDto dto);

    void changeStatus(Long id, Boolean isActive);
}
