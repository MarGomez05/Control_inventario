package com.tamachihua_erp.api.dto.productpresentation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Datos para editar una presentación del producto: solo código y stock mínimo (contrato 5.2)
public record ProductPresentationUpdateRequestDto(

        @NotBlank(message = "El código de la presentación del producto es obligatorio")
        @Size(max = 50, message = "El código no puede superar los 50 caracteres")
        String code,

        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0.01", message = "El stock mínimo debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2,
                message = "El stock mínimo admite hasta 10 enteros y 2 decimales")
        BigDecimal minStock
) {
}
