package com.tamachihua_erp.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiFieldErrorDto> errors
) {
}