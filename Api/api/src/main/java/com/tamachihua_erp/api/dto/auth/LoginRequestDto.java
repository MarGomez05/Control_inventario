package com.tamachihua_erp.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50)
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {}
