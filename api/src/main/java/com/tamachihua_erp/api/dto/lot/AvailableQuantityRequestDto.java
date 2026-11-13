package com.tamachihua_erp.api.dto.lot;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Corrección de la cantidad disponible de un lote (HU-11, contrato 7)
public record AvailableQuantityRequestDto(

        @NotNull(message = "La cantidad disponible es obligatoria")
        @DecimalMin(value = "0.00", message = "La cantidad disponible no puede ser negativa")
        @Digits(integer = 10, fraction = 2,
                message = "La cantidad disponible admite hasta 10 enteros y 2 decimales")
        BigDecimal availableQuantity
) {
}
