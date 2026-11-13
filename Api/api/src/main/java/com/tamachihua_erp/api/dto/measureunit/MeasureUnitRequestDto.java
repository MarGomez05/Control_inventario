package com.tamachihua_erp.api.dto.measureunit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar una unidad de medida
public record MeasureUnitRequestDto(

        @NotBlank(message = "El nombre de la unidad es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String name,

        @NotBlank(message = "El código de la unidad es obligatorio")
        @Size(max = 10, message = "El código no puede superar los 10 caracteres")
        String code
) {
}
