package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.app.AppLowStockItemDto;
import com.tamachihua_erp.api.dto.dashboard.DashboardSummaryDto;
import com.tamachihua_erp.api.dto.dashboard.ExpiringLotItemDto;
import com.tamachihua_erp.api.dto.dashboard.LowStockItemDto;
import com.tamachihua_erp.api.repository.ProductLotRepository;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.projection.ExpiringLotProjection;
import com.tamachihua_erp.api.repository.projection.LowStockProjection;
import com.tamachihua_erp.api.service.DashboardService;
import com.tamachihua_erp.api.util.ExpirationSeverityCalculator;
import com.tamachihua_erp.api.util.StockStatusCalculator;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int EXPIRATION_WINDOW_DAYS = 30;

    private final ProductRepository productRepository;
    private final ProductPresentationRepository productPresentationRepository;
    private final ProductLotRepository productLotRepository;

    @Override
    public DashboardSummaryDto getSummary() {

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(EXPIRATION_WINDOW_DAYS);

        return new DashboardSummaryDto(
                productRepository.countByIsActiveTrue(),
                productPresentationRepository.countLowStock(),
                productLotRepository.countActiveLots(),
                productLotRepository.countExpiringLots(today, limit),
                productLotRepository.countExpiredLots(today)
        );
    }

    @Override
    public Page<LowStockItemDto> findLowStock(Pageable pageable) {

        return productPresentationRepository.findLowStock(pageable)
                .map(this::toLowStockItem);
    }

    @Override
    public List<ExpiringLotItemDto> findExpiringLots() {

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(EXPIRATION_WINDOW_DAYS);

        return productLotRepository.findExpiringLots(limit)
                .stream()
                .map(projection -> toExpiringLotItem(projection, today))
                .toList();
    }

    @Override
    public List<AppLowStockItemDto> findAppLowStock() {

        return productPresentationRepository.findLowStockList()
                .stream()
                .map(projection -> {

                    BigDecimal stock = safe(projection.getCurrentStock());
                    BigDecimal minStock = safe(projection.getMinStock());

                    return new AppLowStockItemDto(
                            projection.getProductId(),
                            projection.getProductName(),
                            projection.getPresentationName(),
                            stock,
                            StockStatusCalculator.calculate(stock, minStock)
                    );
                })
                .toList();
    }

    private LowStockItemDto toLowStockItem(LowStockProjection projection) {

        BigDecimal stock = safe(projection.getCurrentStock());
        BigDecimal minStock = safe(projection.getMinStock());
        BigDecimal missing = minStock.subtract(stock).max(BigDecimal.ZERO);

        return new LowStockItemDto(
                projection.getProductId(),
                projection.getProductCode(),
                projection.getProductName(),
                projection.getPresentationName(),
                stock,
                minStock,
                missing
        );
    }

    private ExpiringLotItemDto toExpiringLotItem(
            ExpiringLotProjection projection,
            LocalDate today
    ) {

        long daysRemaining = ChronoUnit.DAYS.between(
                today,
                projection.getExpirationDate()
        );

        String severity = ExpirationSeverityCalculator.severity(daysRemaining);

        return new ExpiringLotItemDto(
                projection.getLotId(),
                projection.getProductName(),
                projection.getPresentationName(),
                projection.getLotNumber(),
                projection.getExpirationDate(),
                daysRemaining,
                severity,
                ExpirationSeverityCalculator.label(severity)
        );
    }

    private BigDecimal safe(BigDecimal value) {

        return value == null ? BigDecimal.ZERO : value;
    }
}
