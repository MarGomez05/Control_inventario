package com.tamachihua_erp.api.dto.supplier;

import java.time.LocalDateTime;

// Respuesta de la API para un proveedor (contrato 6)
public record SupplierResponseDto(

        Long id,
        String tradeName,
        String legalName,
        String nit,
        String nrc,
        String address,
        String phone,
        String email,
        Integer creditDays,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
