package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.lot.LastCostItemDto;
import com.tamachihua_erp.api.service.ProductLotService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Último costo pagado por presentación (contrato 7). Roles: ADMIN y PURCHASING.
@RestController
@RequestMapping("/api/products/{productId}/last-cost")
@RequiredArgsConstructor
public class ProductLastCostController {

    private final ProductLotService service;

    @GetMapping
    public ResponseEntity<List<LastCostItemDto>> findLastCosts(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(service.findLastCosts(productId));
    }
}
