package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.app.AppProductDetailDto;
import com.tamachihua_erp.api.dto.app.AppProductSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Consultas de solo lectura para la app móvil (HU-13, HU-14)
public interface AppProductService {

    Page<AppProductSummaryDto> search(
            String search,
            Long categoryId,
            Long productTypeId,
            Pageable pageable
    );

    AppProductDetailDto findById(Long id);
}
