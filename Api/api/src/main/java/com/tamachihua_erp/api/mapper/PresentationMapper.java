package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.presentation.PresentationResponseDto;
import com.tamachihua_erp.api.entity.Presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// La entidad se arma en el servicio, porque necesita buscar la unidad de medida (measureUnitId)
@Component
@RequiredArgsConstructor
public class PresentationMapper {

    private final MeasureUnitMapper measureUnitMapper;

    // Convierte una entidad en un Response DTO, con measureUnit {id, name, code}
    public PresentationResponseDto toResponseDto(Presentation presentation) {

        return new PresentationResponseDto(
                presentation.getId(),
                presentation.getName(),
                presentation.getQuantity(),
                measureUnitMapper.toSummaryDto(presentation.getMeasureUnit()),
                presentation.getIsActive(),
                presentation.getCreatedAt(),
                presentation.getUpdatedAt()
        );
    }
}
