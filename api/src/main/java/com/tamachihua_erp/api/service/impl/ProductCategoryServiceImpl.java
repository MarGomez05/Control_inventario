package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.category.ProductCategoryRequestDto;
import com.tamachihua_erp.api.dto.category.ProductCategoryResponseDto;
import com.tamachihua_erp.api.entity.ProductCategory;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.mapper.ProductCategoryMapper;
import com.tamachihua_erp.api.repository.ProductCategoryRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.service.ProductCategoryService;

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
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository repository;
    private final ProductRepository productRepository;
    private final ProductCategoryMapper mapper;

    // Consultar categorías con filtros y paginación
    @Override
    public Page<ProductCategoryResponseDto> findAll(
            String search,
            Boolean isActive,
            Pageable pageable
    ) {

        Specification<ProductCategory> specification =
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

    // Consultar categorías activas para selectores
    @Override
    public List<ProductCategoryResponseDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    // Consultar una categoría por ID
    @Override
    public ProductCategoryResponseDto findById(Long id) {

        ProductCategory category = findCategoryOrThrow(id);

        return mapper.toResponseDto(category);
    }

    // Registrar una categoría
    @Override
    @Transactional
    public ProductCategoryResponseDto create(ProductCategoryRequestDto dto) {

        if (repository.existsByNameIgnoreCase(dto.name())) {
            throw new ResourceConflictException(
                    "Ya existe una categoría con ese nombre"
            );
        }

        ProductCategory category = mapper.toEntity(dto);

        ProductCategory savedCategory = repository.save(category);

        return mapper.toResponseDto(savedCategory);
    }

    // Actualizar una categoría
    @Override
    @Transactional
    public ProductCategoryResponseDto update(
            Long id,
            ProductCategoryRequestDto dto
    ) {

        ProductCategory existingCategory = findCategoryOrThrow(id);

        if (repository.existsByNameIgnoreCaseAndIdNot(dto.name(), id)) {
            throw new ResourceConflictException(
                    "Ya existe otra categoría con ese nombre"
            );
        }

        existingCategory.setName(dto.name());
        existingCategory.setDescription(dto.description());

        ProductCategory updatedCategory = repository.save(existingCategory);

        return mapper.toResponseDto(updatedCategory);
    }

    // Cambiar el estado de una categoría
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        ProductCategory category = findCategoryOrThrow(id);

        // No se desactiva una categoría que tenga productos activos (409)
        if (Boolean.FALSE.equals(isActive)
                && productRepository.existsByProductCategoryIdAndIsActiveTrue(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar la categoría porque tiene productos activos"
            );
        }

        category.setIsActive(isActive);

        repository.save(category);
    }

    // Metodo auxiliar para buscar una categoría
    private ProductCategory findCategoryOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la categoría con ID: " + id
                ));
    }
}
