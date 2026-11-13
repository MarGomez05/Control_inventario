package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.supplier.SupplierOptionDto;
import com.tamachihua_erp.api.dto.supplier.SupplierResponseDto;
import com.tamachihua_erp.api.dto.supplier.SupplierSummaryDto;
import com.tamachihua_erp.api.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierResponseDto toResponseDto(Supplier supplier) {

        return new SupplierResponseDto(
                supplier.getId(),
                supplier.getTradeName(),
                supplier.getLegalName(),
                supplier.getNit(),
                supplier.getNrc(),
                supplier.getAddress(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getCreditDays(),
                supplier.getIsActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

    public SupplierSummaryDto toSummaryDto(Supplier supplier) {

        return new SupplierSummaryDto(supplier.getId(), supplier.getTradeName());
    }

    public SupplierOptionDto toOptionDto(Supplier supplier) {

        return new SupplierOptionDto(supplier.getId(), supplier.getTradeName());
    }
}
