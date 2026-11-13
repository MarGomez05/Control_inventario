package com.tamachihua_erp.api.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos que envía el panel para crear o editar un proveedor (contrato 6, HU-09)
public record SupplierRequestDto(

        @NotBlank(message = "El nombre comercial es obligatorio")
        @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres")
        String tradeName,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres")
        String legalName,

        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 17, message = "El NIT no puede superar los 17 caracteres")
        @Pattern(
                regexp = "^(\\d{4}-\\d{6}-\\d{3}-\\d|\\d{8}-\\d)$",
                message = "El NIT debe tener el formato 0000-000000-000-0 o 00000000-0"
        )
        String nit,

        @Size(max = 10, message = "El NRC no puede superar los 10 caracteres")
        @Pattern(
                regexp = "^$|^\\d{1,7}-\\d$",
                message = "El NRC debe tener el formato 000000-0"
        )
        String nrc,

        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String address,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        @Pattern(
                regexp = "^$|^[0-9 +-]+$",
                message = "El teléfono solo admite dígitos, espacios, + y -"
        )
        String phone,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String email,

        @NotNull(message = "Los días de crédito son obligatorios")
        @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
        Integer creditDays
) {
}
