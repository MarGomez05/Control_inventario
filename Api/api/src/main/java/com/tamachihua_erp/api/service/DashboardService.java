package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.app.AppLowStockItemDto;
import com.tamachihua_erp.api.dto.dashboard.DashboardSummaryDto;
import com.tamachihua_erp.api.dto.dashboard.ExpiringLotItemDto;
import com.tamachihua_erp.api.dto.dashboard.LowStockItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Indicadores y alertas del inventario (HU-12, contrato 8)
public interface DashboardService {

    DashboardSummaryDto getSummary();

    Page<LowStockItemDto> findLowStock(Pageable pageable);

    List<ExpiringLotItemDto> findExpiringLots();

    List<AppLowStockItemDto> findAppLowStock();
}
