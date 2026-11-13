package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.measureunit.MeasureUnitRequestDto;
import com.tamachihua_erp.api.dto.measureunit.MeasureUnitResponseDto;
import com.tamachihua_erp.api.entity.MeasureUnit;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.MeasureUnitMapper;
import com.tamachihua_erp.api.repository.MeasureUnitRepository;
import com.tamachihua_erp.api.repository.PresentationRepository;
import com.tamachihua_erp.api.service.MeasureUnitService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasureUnitServiceImpl implements MeasureUnitService {

    private final MeasureUnitRepository repository;
    private final PresentationRepository presentationRepository;
    private final MeasureUnitMapper mapper;

    // Listado paginado con filtros: search (nombre o código) e isActive
    @Override
    public Page<MeasureUnitResponseDto> findAll(String search, Boolean isActive, Pageable pageable) {

        Specification<MeasureUnit> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("code")), pattern)
                    )
            );
        }

        if (isActive != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive)
            );
        }

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }

    // Unidades activas para los selectores del panel
    @Override
    public List<MeasureUnitResponseDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public MeasureUnitResponseDto findById(Long id) {

        return mapper.toResponseDto(findMeasureUnitOrThrow(id));
    }

    @Override
    @Transactional
    public MeasureUnitResponseDto create(MeasureUnitRequestDto dto) {

        validateUniqueness(dto, null);

        MeasureUnit saved = repository.save(mapper.toEntity(dto));

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public MeasureUnitResponseDto update(Long id, MeasureUnitRequestDto dto) {

        MeasureUnit existing = findMeasureUnitOrThrow(id);

        validateUniqueness(dto, id);

        existing.setName(dto.name().trim());
        existing.setCode(dto.code().trim());

        return mapper.toResponseDto(repository.save(existing));
    }

    // No se desactiva una unidad que tenga presentaciones activas (409)
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        MeasureUnit measureUnit = findMeasureUnitOrThrow(id);

        if (Boolean.FALSE.equals(isActive)
                && presentationRepository.existsByMeasureUnitIdAndIsActiveTrue(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar la unidad porque tiene presentaciones activas"
            );
        }

        measureUnit.setIsActive(isActive);

        repository.save(measureUnit);
    }

    // Nombre y código únicos sin distinguir mayúsculas (id = null al crear)
    private void validateUniqueness(MeasureUnitRequestDto dto, Long id) {

        String name = dto.name().trim();
        String code = dto.code().trim();

        boolean nameTaken = id == null
                ? repository.existsByNameIgnoreCase(name)
                : repository.existsByNameIgnoreCaseAndIdNot(name, id);

        if (nameTaken) {
            throw new ResourceConflictException("Ya existe una unidad de medida con ese nombre");
        }

        boolean codeTaken = id == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndIdNot(code, id);

        if (codeTaken) {
            throw new ResourceConflictException("Ya existe una unidad de medida con ese código");
        }
    }

    private MeasureUnit findMeasureUnitOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la unidad de medida con ID: " + id
                ));
    }
}
