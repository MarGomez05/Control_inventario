package com.tamachihua_erp.api.dto.productpresentation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Datos para asignar una presentación a un producto (contrato 5.2)
public record ProductPresentationRequestDto(

        @NotNull(message = "La presentación es obligatoria")
        Long presentationId,

        @NotBlank(message = "El código de la presentación del producto es obligatorio")
        @Size(max = 50, message = "El código no puede superar los 50 caracteres")
        String code,

        // NUMERIC(12,2) y CHECK min_stock > 0
        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.01", message = "El stock mínimo debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2,
                message = "El stock mínimo admite hasta 10 enteros y 2 decimales")
        BigDecimal minStock
) {
}
