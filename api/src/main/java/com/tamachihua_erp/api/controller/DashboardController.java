package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.dashboard.DashboardSummaryDto;
import com.tamachihua_erp.api.dto.dashboard.ExpiringLotItemDto;
import com.tamachihua_erp.api.dto.dashboard.LowStockItemDto;
import com.tamachihua_erp.api.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Indicadores y alertas del inventario (HU-12, contrato 8)
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<PageResponseDto<LowStockItemDto>> findLowStock(
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        // La consulta nativa ya define el orden; se descarta el sort del cliente.
        int size = Math.min(pageable.getPageSize(), 100);
        Pageable limitedPageable = PageRequest.of(pageable.getPageNumber(), size);

        return ResponseEntity.ok(PageResponseDto.from(
                service.findLowStock(limitedPageable)
        ));
    }

    @GetMapping("/expiring-lots")
    public ResponseEntity<List<ExpiringLotItemDto>> findExpiringLots() {
        return ResponseEntity.ok(service.findExpiringLots());
    }
}
