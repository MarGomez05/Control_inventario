package com.tamachihua_erp.api.dto.measureunit;

// Versión corta de la unidad, para incluirla dentro de otras respuestas: measureUnit {id, name, code}
public record MeasureUnitSummaryDto(

        Long id,
        String name,
        String code
) {
}
