package com.tamachihua_erp.api.dto.common;

import jakarta.validation.constraints.NotNull;

// Cuerpo del PATCH /{id}/status de los catálogos: { "isActive": false }
public record StatusRequestDto(

        @NotNull(message = "El estado es obligatorio")
        Boolean isActive

) {
}
