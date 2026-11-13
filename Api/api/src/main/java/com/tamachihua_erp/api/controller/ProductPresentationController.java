package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationRequestDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationUpdateRequestDto;
import com.tamachihua_erp.api.service.ProductPresentationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Presentaciones de un producto con su stock mínimo (HU-07, contrato 5.2)
@RestController
@RequestMapping("/api/products/{productId}/presentations")
@RequiredArgsConstructor
public class ProductPresentationController {

    private final ProductPresentationService service;

    @GetMapping
    public ResponseEntity<List<ProductPresentationResponseDto>> findByProductId(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(service.findByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<ProductPresentationResponseDto> create(
            @PathVariable Long productId,
            @Valid @RequestBody ProductPresentationRequestDto dto
    ) {
        ProductPresentationResponseDto created = service.create(productId, dto);

        return ResponseEntity
                .created(URI.create(
                        "/api/products/" + productId + "/presentations/" + created.id()
                ))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductPresentationResponseDto> update(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody ProductPresentationUpdateRequestDto dto
    ) {
        return ResponseEntity.ok(service.update(productId, id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody StatusRequestDto dto
    ) {
        service.changeStatus(productId, id, dto.isActive());
        return ResponseEntity.noContent().build();
    }
}
