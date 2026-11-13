package com.tamachihua_erp.api.dto.role;

// Versión corta del rol para la respuesta de un usuario: role {id, name}
public record RoleSummaryDto(

        Long id,
        String name
) {
}
