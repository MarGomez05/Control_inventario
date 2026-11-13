package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.dto.model.ProductModelRequestDto;
import com.tamachihua_erp.api.dto.model.ProductModelResponseDto;
import com.tamachihua_erp.api.service.ProductModelService;
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

// Catálogo de modelos (HU-04). Mismo patrón que /api/categories, filtrable por brandId (contrato 4.4)
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ProductModelController {

    private final ProductModelService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<ProductModelResponseDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long brandId,
            @PageableDefault(page = 0, size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        // Tamaño máximo de página: 100 (contrato 1.4)
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.findAll(search, isActive, brandId, limitedPageable)
        ));
    }

    // Para el combo de modelos en el formulario de producto: /api/models/options?brandId=5
    @GetMapping("/options")
    public ResponseEntity<List<ProductModelResponseDto>> findActiveOptions(
            @RequestParam(required = false) Long brandId
    ) {
        return ResponseEntity.ok(service.findActiveOptions(brandId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductModelResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductModelResponseDto> create(@Valid @RequestBody ProductModelRequestDto dto) {

        ProductModelResponseDto created = service.create(dto);

        return ResponseEntity
                .created(URI.create("/api/models/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductModelResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductModelRequestDto dto
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
