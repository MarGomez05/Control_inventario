package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.dto.supplier.SupplierOptionDto;
import com.tamachihua_erp.api.dto.supplier.SupplierRequestDto;
import com.tamachihua_erp.api.dto.supplier.SupplierResponseDto;
import com.tamachihua_erp.api.service.SupplierService;

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

// Proveedores (HU-09, contrato 6). Roles: ADMIN y PURCHASING.
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<SupplierResponseDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20, sort = "tradeName", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.findAll(search, isActive, limitedPageable)
        ));
    }

    @GetMapping("/options")
    public ResponseEntity<List<SupplierOptionDto>> findActiveOptions() {
        return ResponseEntity.ok(service.findActiveOptions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDto> create(
            @Valid @RequestBody SupplierRequestDto dto
    ) {
        SupplierResponseDto created = service.create(dto);

        return ResponseEntity
                .created(URI.create("/api/suppliers/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDto dto
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
