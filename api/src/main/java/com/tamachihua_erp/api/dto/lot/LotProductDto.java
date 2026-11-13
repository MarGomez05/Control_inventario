package com.tamachihua_erp.api.dto.lot;

// Producto dentro de la respuesta de un lote: product {id, code, name}
public record LotProductDto(

        Long id,
        String code,
        String name
) {
}
