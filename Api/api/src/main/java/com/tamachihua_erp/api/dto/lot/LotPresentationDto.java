package com.tamachihua_erp.api.dto.lot;

// Presentación dentro de la respuesta de un lote: presentation {productPresentationId, name}
public record LotPresentationDto(

        Long productPresentationId,
        String name
) {
}
