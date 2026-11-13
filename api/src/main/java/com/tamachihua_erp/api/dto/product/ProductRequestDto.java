package com.tamachihua_erp.api.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar un producto (contrato 5.1)
public record ProductRequestDto(

        @NotBlank(message = "El código del producto es obligatorio")
        @Size(max = 50, message = "El código no puede superar los 50 caracteres")
        String code,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String name,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotNull(message = "El tipo de producto es obligatorio")
        Long productTypeId,

        // Opcional: el producto puede no tener modelo
        Long productModelId,

        @NotNull(message = "Debe indicar si el producto requiere vencimiento")
        Boolean requiresExpiration
) {
}
