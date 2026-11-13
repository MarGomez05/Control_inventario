package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.app.AppProductDetailDto;
import com.tamachihua_erp.api.dto.app.AppProductSummaryDto;
import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.service.AppProductService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Consulta de productos para la app móvil (HU-13, HU-14, contrato 10).
// Las rutas son públicas hasta HU-17 (Sprint 3).
@RestController
@RequestMapping("/api/app/products")
@RequiredArgsConstructor
public class AppProductController {

    private final AppProductService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<AppProductSummaryDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productTypeId,
            @PageableDefault(page = 0, size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        // Tamaño máximo de página de la app: 50 (contrato 10)
        Pageable limitedPageable = pageable.getPageSize() > 50
                ? PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.search(search, categoryId, productTypeId, limitedPageable)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppProductDetailDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
