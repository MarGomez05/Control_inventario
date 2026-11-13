package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.app.AppCatalogItemDto;
import com.tamachihua_erp.api.service.ProductCategoryService;
import com.tamachihua_erp.api.service.ProductTypeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catálogos activos para los filtros de la app móvil (contrato 10)
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppCatalogController {

    private final ProductCategoryService categoryService;
    private final ProductTypeService productTypeService;

    @GetMapping("/categories")
    public ResponseEntity<List<AppCatalogItemDto>> findCategories() {

        return ResponseEntity.ok(
                categoryService.findActiveOptions()
                        .stream()
                        .map(category -> new AppCatalogItemDto(category.id(), category.name()))
                        .toList()
        );
    }

    @GetMapping("/product-types")
    public ResponseEntity<List<AppCatalogItemDto>> findProductTypes() {

        return ResponseEntity.ok(
                productTypeService.findActiveOptions()
                        .stream()
                        .map(type -> new AppCatalogItemDto(type.id(), type.name()))
                        .toList()
        );
    }
}
