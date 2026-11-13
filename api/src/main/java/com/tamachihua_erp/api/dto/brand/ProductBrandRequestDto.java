package com.tamachihua_erp.api.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar una marca
public record ProductBrandRequestDto(

        @NotBlank(message = "El nombre de la marca es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name
) {
}
