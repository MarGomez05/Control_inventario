package com.tamachihua_erp.api.dto.producttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar un tipo de producto (contrato 4.2)
public record ProductTypeRequestDto(

        @NotBlank(message = "El nombre del tipo de producto es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description
) {
}
