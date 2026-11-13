package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.brand.ProductBrandRequestDto;
import com.tamachihua_erp.api.dto.brand.ProductBrandResponseDto;
import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.service.ProductBrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Catálogo de marcas (HU-04). Mismo patrón que /api/categories (contrato 1.5 y 4.3)
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class ProductBrandController {

    private final ProductBrandService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<ProductBrandResponseDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        // Tamaño máximo de página: 100 (contrato 1.4)
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(service.findAll(search, isActive, limitedPageable)));
    }

    @GetMapping("/options")
    public ResponseEntity<List<ProductBrandResponseDto>> findActiveOptions() {
        return ResponseEntity.ok(service.findActiveOptions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductBrandResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductBrandResponseDto> create(@Valid @RequestBody ProductBrandRequestDto dto) {

        ProductBrandResponseDto created = service.create(dto);

        return ResponseEntity
                .created(URI.create("/api/brands/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductBrandResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductBrandRequestDto dto
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
