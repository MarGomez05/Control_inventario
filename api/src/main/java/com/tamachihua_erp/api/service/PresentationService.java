package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.presentation.PresentationRequestDto;
import com.tamachihua_erp.api.dto.presentation.PresentationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de presentaciones (HU-05)
public interface PresentationService {

    Page<PresentationResponseDto> findAll(
            String search,
            Boolean isActive,
            Long measureUnitId,
            Pageable pageable
    );

    List<PresentationResponseDto> findActiveOptions();

    PresentationResponseDto findById(Long id);

    PresentationResponseDto create(PresentationRequestDto dto);

    PresentationResponseDto update(Long id, PresentationRequestDto dto);

    // Activar o desactivar (borrado lógico)
    void changeStatus(Long id, Boolean isActive);
}
