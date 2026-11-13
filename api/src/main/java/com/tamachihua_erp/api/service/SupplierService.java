package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.supplier.SupplierOptionDto;
import com.tamachihua_erp.api.dto.supplier.SupplierRequestDto;
import com.tamachihua_erp.api.dto.supplier.SupplierResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de proveedores (HU-09)
public interface SupplierService {

    Page<SupplierResponseDto> findAll(
            String search,
            Boolean isActive,
            Pageable pageable
    );

    List<SupplierOptionDto> findActiveOptions();

    SupplierResponseDto findById(Long id);

    SupplierResponseDto create(SupplierRequestDto dto);

    SupplierResponseDto update(Long id, SupplierRequestDto dto);

    void changeStatus(Long id, Boolean isActive);
}
