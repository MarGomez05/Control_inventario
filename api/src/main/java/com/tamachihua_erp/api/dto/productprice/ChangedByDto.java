package com.tamachihua_erp.api.dto.productprice;

// Usuario que registró el cambio de precio: changedBy {id, fullName}
public record ChangedByDto(

        Long id,
        String fullName
) {
}
