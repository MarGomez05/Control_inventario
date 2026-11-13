package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.lot.LastCostItemDto;
import com.tamachihua_erp.api.dto.lot.LotRequestDto;
import com.tamachihua_erp.api.dto.lot.LotView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

// Registro y consulta de lotes (HU-10, HU-11)
public interface ProductLotService {

    Page<LotView> findAll(
            Long productId,
            Long supplierId,
            String invoiceNumber,
            Boolean onlyAvailable,
            Pageable pageable,
            boolean includeCosts
    );

    LotView findById(Long id, boolean includeCosts);

    LotView create(LotRequestDto dto, Long userId, boolean includeCosts);

    void updateAvailableQuantity(Long id, BigDecimal availableQuantity);

    List<LastCostItemDto> findLastCosts(Long productId);
}
