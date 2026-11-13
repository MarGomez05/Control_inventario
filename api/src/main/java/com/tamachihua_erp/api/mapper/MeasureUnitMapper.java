package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.measureunit.MeasureUnitRequestDto;
import com.tamachihua_erp.api.dto.measureunit.MeasureUnitResponseDto;
import com.tamachihua_erp.api.dto.measureunit.MeasureUnitSummaryDto;
import com.tamachihua_erp.api.entity.MeasureUnit;
import org.springframework.stereotype.Component;

@Component
public class MeasureUnitMapper {

    // Convierte un Request DTO en una entidad nueva
    public MeasureUnit toEntity(MeasureUnitRequestDto dto) {

        MeasureUnit measureUnit = new MeasureUnit();

        measureUnit.setName(dto.name().trim());
        measureUnit.setCode(dto.code().trim());

        return measureUnit;
    }

    // Convierte una entidad en un Response DTO
    public MeasureUnitResponseDto toResponseDto(MeasureUnit measureUnit) {

        return new MeasureUnitResponseDto(
                measureUnit.getId(),
                measureUnit.getName(),
                measureUnit.getCode(),
                measureUnit.getIsActive(),
                measureUnit.getCreatedAt(),
                measureUnit.getUpdatedAt()
        );
    }

    // Versión corta para incluirla en la respuesta de una presentación
    public MeasureUnitSummaryDto toSummaryDto(MeasureUnit measureUnit) {

        return new MeasureUnitSummaryDto(
                measureUnit.getId(),
                measureUnit.getName(),
                measureUnit.getCode()
        );
    }
}
