package com.tamachihua_erp.api.dto.presentation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Datos que envía el panel para crear o editar una presentación
public record PresentationRequestDto(

        @NotBlank(message = "El nombre de la presentación es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotNull(message = "La unidad de medida es obligatoria")
        Long measureUnitId,

        // NUMERIC(10,2) y CHECK quantity > 0
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2, message = "La cantidad admite hasta 8 enteros y 2 decimales")
        BigDecimal quantity
) {
}
