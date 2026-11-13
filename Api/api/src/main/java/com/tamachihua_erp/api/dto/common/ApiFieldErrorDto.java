package com.tamachihua_erp.api.dto.common;

public record ApiFieldErrorDto(
        String field,
        String message
) {
}