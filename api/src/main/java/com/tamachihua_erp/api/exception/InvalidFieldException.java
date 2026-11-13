package com.tamachihua_erp.api.exception;

import lombok.Getter;

// Regla de negocio sobre un campo del cuerpo (por ejemplo, measureUnitId que no existe o está inactivo).
// El GlobalExceptionHandler la convierte en HTTP 400 con errors[] = [{ field, message }] (contrato, sección 1.2).
@Getter
public class InvalidFieldException extends RuntimeException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }
}
