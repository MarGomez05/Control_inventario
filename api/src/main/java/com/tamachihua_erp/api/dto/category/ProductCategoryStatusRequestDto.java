package com.tamachihua_erp.api.dto.category;

import jakarta.validation.constraints.NotNull;

// Utilizada en el patch para cambiar el estado de la categoría
public record ProductCategoryStatusRequestDto(

        @NotNull(message = "El estado de la categoría es obligatorio")
        Boolean isActive

) {
}
