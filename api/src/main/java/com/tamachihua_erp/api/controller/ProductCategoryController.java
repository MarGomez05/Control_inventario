package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.category.ProductCategoryRequestDto;
import com.tamachihua_erp.api.dto.category.ProductCategoryResponseDto;
import com.tamachihua_erp.api.dto.category.ProductCategoryStatusRequestDto;
import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.service.ProductCategoryService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Están anotación le dan a entender a spring que es un controlador que utiliza HTTP
@RestController
// Ruta de acceso a los recursos
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService service;

    // GET: Obtener categorías con filtros y paginación
    @GetMapping
    public ResponseEntity<PageResponseDto<ProductCategoryResponseDto>> findAll(

            @RequestParam(required = false) String search,

            @RequestParam(required = false) Boolean isActive,

            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            ) Pageable pageable

    ) {

        Pageable limitedPageable = pageable;

        if (pageable.getPageSize() > 100) {

            limitedPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    100,
                    pageable.getSort()
            );
        }

        Page<ProductCategoryResponseDto> result =
                service.findAll(search, isActive, limitedPageable);

        return ResponseEntity.ok(PageResponseDto.from(result));
    }

    // GET: Obtener categorías activas para selectores
    @GetMapping("/options")
    public ResponseEntity<List<ProductCategoryResponseDto>> findActiveOptions() {

        return ResponseEntity.ok(service.findActiveOptions());
    }

    // GET: Obtener categoría por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryResponseDto> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(service.findById(id));
    }

    // POST: Registrar una categoría
    @PostMapping
    public ResponseEntity<ProductCategoryResponseDto> create(
            @Valid @RequestBody ProductCategoryRequestDto dto
    ) {

        ProductCategoryResponseDto createdCategory = service.create(dto);

        URI location = URI.create("/api/categories/" + createdCategory.id());

        return ResponseEntity
                .created(location)
                .body(createdCategory);
    }

    // PUT: Actualizar una categoría
    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryRequestDto dto
    ) {

        return ResponseEntity.ok(service.update(id, dto));
    }

    // PATCH: Cambiar estado de una categoría
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryStatusRequestDto dto
    ) {

        service.changeStatus(id, dto.isActive());

        return ResponseEntity.noContent().build();
    }
}

