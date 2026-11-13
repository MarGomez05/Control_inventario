package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.brand.ProductBrandRequestDto;
import com.tamachihua_erp.api.dto.brand.ProductBrandResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de marcas (HU-04)
public interface ProductBrandService {

    Page<ProductBrandResponseDto> findAll(String search, Boolean isActive, Pageable pageable);

    List<ProductBrandResponseDto> findActiveOptions();

    ProductBrandResponseDto findById(Long id);

    ProductBrandResponseDto create(ProductBrandRequestDto dto);

    ProductBrandResponseDto update(Long id, ProductBrandRequestDto dto);

    void changeStatus(Long id, Boolean isActive);
}
