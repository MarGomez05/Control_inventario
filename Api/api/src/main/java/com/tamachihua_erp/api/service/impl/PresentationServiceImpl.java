package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.presentation.PresentationRequestDto;
import com.tamachihua_erp.api.dto.presentation.PresentationResponseDto;
import com.tamachihua_erp.api.entity.MeasureUnit;
import com.tamachihua_erp.api.entity.Presentation;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.PresentationMapper;
import com.tamachihua_erp.api.repository.MeasureUnitRepository;
import com.tamachihua_erp.api.repository.PresentationRepository;
import com.tamachihua_erp.api.service.PresentationService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PresentationServiceImpl implements PresentationService {

    private final PresentationRepository repository;
    private final MeasureUnitRepository measureUnitRepository;
    private final PresentationMapper mapper;

    // Listado paginado con filtros: search (nombre), isActive y measureUnitId
    @Override
    public Page<PresentationResponseDto> findAll(
            String search,
            Boolean isActive,
            Long measureUnitId,
            Pageable pageable
    ) {

        Specification<Presentation> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern)
            );
        }

        if (isActive != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive)
            );
        }

        if (measureUnitId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("measureUnit").get("id"), measureUnitId)
            );
        }

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }

    // Presentaciones activas para los selectores (por ejemplo, al asignar presentaciones a un producto)
    @Override
    public List<PresentationResponseDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public PresentationResponseDto findById(Long id) {

        return mapper.toResponseDto(findPresentationOrThrow(id));
    }

    @Override
    @Transactional
    public PresentationResponseDto create(PresentationRequestDto dto) {

        String name = dto.name().trim();

        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("Ya existe una presentación con ese nombre");
        }

        Presentation presentation = new Presentation();
        presentation.setName(name);
        presentation.setQuantity(dto.quantity());
        presentation.setMeasureUnit(findAssignableMeasureUnit(dto.measureUnitId(), null));

        return mapper.toResponseDto(repository.save(presentation));
    }

    @Override
    @Transactional
    public PresentationResponseDto update(Long id, PresentationRequestDto dto) {

        Presentation existing = findPresentationOrThrow(id);
        String name = dto.name().trim();

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("Ya existe otra presentación con ese nombre");
        }

        existing.setName(name);
        existing.setQuantity(dto.quantity());
        existing.setMeasureUnit(findAssignableMeasureUnit(dto.measureUnitId(), existing.getMeasureUnit()));

        return mapper.toResponseDto(repository.save(existing));
    }

    // Desactivar: no si está asignada a productos activos. Activar: no si su unidad está inactiva.
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        Presentation presentation = findPresentationOrThrow(id);

        if (Boolean.FALSE.equals(isActive) && repository.isUsedByActiveProducts(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar la presentación porque está asignada a productos activos"
            );
        }

        if (Boolean.TRUE.equals(isActive)
                && !Boolean.TRUE.equals(presentation.getMeasureUnit().getIsActive())) {
            throw new ResourceConflictException(
                    "No se puede activar la presentación porque su unidad de medida está inactiva"
            );
        }

        presentation.setIsActive(isActive);

        repository.save(presentation);
    }

    // La unidad debe existir y estar activa (contrato 4.6). Al editar se acepta la unidad que ya tenía.
    private MeasureUnit findAssignableMeasureUnit(Long measureUnitId, MeasureUnit current) {

        MeasureUnit measureUnit = measureUnitRepository.findById(measureUnitId)
                .orElseThrow(() -> new InvalidFieldException(
                        "measureUnitId", "La unidad de medida no existe o está inactiva"
                ));

        boolean sameAsCurrent = current != null && Objects.equals(current.getId(), measureUnit.getId());

        if (!sameAsCurrent && !Boolean.TRUE.equals(measureUnit.getIsActive())) {
            throw new InvalidFieldException(
                    "measureUnitId", "La unidad de medida no existe o está inactiva"
            );
        }

        return measureUnit;
    }

    private Presentation findPresentationOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la presentación con ID: " + id
                ));
    }
}
