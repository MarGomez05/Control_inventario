package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.producttype.ProductTypeRequestDto;
import com.tamachihua_erp.api.dto.producttype.ProductTypeResponseDto;
import com.tamachihua_erp.api.entity.ProductType;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductTypeMapper;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.ProductTypeRepository;
import com.tamachihua_erp.api.service.ProductTypeService;

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
public class ProductTypeServiceImpl implements ProductTypeService {

    private final ProductTypeRepository repository;
    private final ProductRepository productRepository;
    private final ProductTypeMapper mapper;

    @Override
    public Page<ProductTypeResponseDto> findAll(
            String search,
            Boolean isActive,
            Pageable pageable
    ) {

        Specification<ProductType> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern)
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

    @Override
    public List<ProductTypeResponseDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public ProductTypeResponseDto findById(Long id) {

        return mapper.toResponseDto(findTypeOrThrow(id));
    }

    @Override
    @Transactional
    public ProductTypeResponseDto create(ProductTypeRequestDto dto) {

        String name = dto.name().trim();

        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("Ya existe un tipo de producto con ese nombre");
        }

        return mapper.toResponseDto(repository.save(mapper.toEntity(dto)));
    }

    @Override
    @Transactional
    public ProductTypeResponseDto update(Long id, ProductTypeRequestDto dto) {

        ProductType existing = findTypeOrThrow(id);
        String name = dto.name().trim();

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("Ya existe otro tipo de producto con ese nombre");
        }

        existing.setName(name);
        existing.setDescription(dto.description());

        return mapper.toResponseDto(repository.save(existing));
    }

    // No se desactiva un tipo con productos activos (regla común del contrato 4)
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        ProductType type = findTypeOrThrow(id);

        if (Boolean.FALSE.equals(isActive)
                && productRepository.existsByProductTypeIdAndIsActiveTrue(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar el tipo de producto porque tiene productos activos"
            );
        }

        type.setIsActive(isActive);

        repository.save(type);
    }

    private ProductType findTypeOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de producto con ID: " + id
                ));
    }
}
