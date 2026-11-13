package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.producttype.ProductTypeRequestDto;
import com.tamachihua_erp.api.dto.producttype.ProductTypeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de tipos de producto (contrato 4.2, HU-20)
public interface ProductTypeService {

    Page<ProductTypeResponseDto> findAll(String search, Boolean isActive, Pageable pageable);

    List<ProductTypeResponseDto> findActiveOptions();

    ProductTypeResponseDto findById(Long id);

    ProductTypeResponseDto create(ProductTypeRequestDto dto);

    ProductTypeResponseDto update(Long id, ProductTypeRequestDto dto);

    void changeStatus(Long id, Boolean isActive);
}
