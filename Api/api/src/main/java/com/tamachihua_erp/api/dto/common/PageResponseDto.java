package com.tamachihua_erp.api.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

// Formato de como se mostrará las paginaciones
public record PageResponseDto<T>(

        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static <T> PageResponseDto<T> from(Page<T> result) {

        return new PageResponseDto<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}

