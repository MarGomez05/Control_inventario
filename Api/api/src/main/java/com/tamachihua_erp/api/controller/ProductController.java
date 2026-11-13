package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.dto.product.ProductDetailDto;
import com.tamachihua_erp.api.dto.product.ProductListItemDto;
import com.tamachihua_erp.api.dto.product.ProductRequestDto;
import com.tamachihua_erp.api.service.ProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// Productos del inventario (HU-06, contrato 5.1)
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<ProductListItemDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productTypeId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        // Tamaño máximo de página: 100 (contrato 1.4)
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.findAll(search, categoryId, productTypeId, isActive, limitedPageable)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDetailDto> create(
            @Valid @RequestBody ProductRequestDto dto
    ) {
        ProductDetailDto created = service.create(dto);

        return ResponseEntity
                .created(URI.create("/api/products/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto dto
    ) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequestDto dto
    ) {
        service.changeStatus(id, dto.isActive());
        return ResponseEntity.noContent().build();
    }
}
