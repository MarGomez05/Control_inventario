package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.app.AppLowStockItemDto;
import com.tamachihua_erp.api.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Alertas de bajo stock para la app móvil (HU-16, contrato 10)
@RestController
@RequestMapping("/api/app/alerts")
@RequiredArgsConstructor
public class AppAlertController {

    private final DashboardService service;

    @GetMapping("/low-stock")
    public ResponseEntity<List<AppLowStockItemDto>> findLowStock() {
        return ResponseEntity.ok(service.findAppLowStock());
    }
}
