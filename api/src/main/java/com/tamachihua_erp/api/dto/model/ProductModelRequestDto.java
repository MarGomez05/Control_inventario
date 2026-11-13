package com.tamachihua_erp.api.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar un modelo (contrato 4.4)
public record ProductModelRequestDto(

        @NotNull(message = "La marca es obligatoria")
        Long brandId,

        @NotBlank(message = "El nombre del modelo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name
) {
}
