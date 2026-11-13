package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.measureunit.MeasureUnitRequestDto;
import com.tamachihua_erp.api.dto.measureunit.MeasureUnitResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Operaciones del catálogo de unidades de medida (HU-05)
public interface MeasureUnitService {

    Page<MeasureUnitResponseDto> findAll(String search, Boolean isActive, Pageable pageable);

    List<MeasureUnitResponseDto> findActiveOptions();

    MeasureUnitResponseDto findById(Long id);

    MeasureUnitResponseDto create(MeasureUnitRequestDto dto);

    MeasureUnitResponseDto update(Long id, MeasureUnitRequestDto dto);

    // Activar o desactivar (borrado lógico)
    void changeStatus(Long id, Boolean isActive);
}
