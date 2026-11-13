package com.tamachihua_erp.api.dto.lot;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Datos para registrar el ingreso de un lote (contrato 7, HU-10)
public record LotRequestDto(

        @NotNull(message = "La presentación del producto es obligatoria")
        Long productPresentationId,

        @NotNull(message = "El proveedor es obligatorio")
        Long supplierId,

        @NotBlank(message = "El número de factura es obligatorio")
        @Size(max = 50, message = "El número de factura no puede superar los 50 caracteres")
        String invoiceNumber,

        @NotNull(message = "La fecha de la factura es obligatoria")
        @PastOrPresent(message = "La fecha de la factura no puede ser futura")
        LocalDate invoiceDate,

        @NotBlank(message = "El número de lote es obligatorio")
        @Size(max = 50, message = "El número de lote no puede superar los 50 caracteres")
        String lotNumber,

        @NotNull(message = "La cantidad recibida es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad recibida debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2,
                message = "La cantidad recibida admite hasta 10 enteros y 2 decimales")
        BigDecimal receivedQuantity,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
        @Digits(integer = 8, fraction = 2,
                message = "El costo unitario admite hasta 8 enteros y 2 decimales")
        BigDecimal unitCost,

        // Obligatoria si el producto tiene requiresExpiration = true (D-01)
        LocalDate expirationDate,

        @Size(max = 200, message = "Las notas no pueden superar los 200 caracteres")
        String notes
) {
}
