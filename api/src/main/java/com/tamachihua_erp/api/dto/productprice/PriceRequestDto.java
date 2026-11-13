package com.tamachihua_erp.api.dto.productprice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Cuerpo del POST de precios: { "price": 19.75 } (contrato 5.3)
public record PriceRequestDto(

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2,
                message = "El precio admite hasta 8 enteros y 2 decimales")
        BigDecimal price
) {
}
