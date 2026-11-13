package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.brand.ProductBrandRequestDto;
import com.tamachihua_erp.api.dto.brand.ProductBrandResponseDto;
import com.tamachihua_erp.api.entity.ProductBrand;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductBrandMapper;
import com.tamachihua_erp.api.repository.ProductBrandRepository;
import com.tamachihua_erp.api.repository.ProductModelRepository;
import com.tamachihua_erp.api.service.ProductBrandService;

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
public class ProductBrandServiceImpl implements ProductBrandService {

    private final ProductBrandRepository repository;
    private final ProductModelRepository modelRepository;
    private final ProductBrandMapper mapper;

    // Listado paginado con filtros: search (nombre) e isActive
    @Override
    public Page<ProductBrandResponseDto> findAll(String search, Boolean isActive, Pageable pageable) {

        Specification<ProductBrand> specification =
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

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    public List<ProductBrandResponseDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public ProductBrandResponseDto findById(Long id) {

        return mapper.toResponseDto(findBrandOrThrow(id));
    }

    @Override
    @Transactional
    public ProductBrandResponseDto create(ProductBrandRequestDto dto) {

        if (repository.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ResourceConflictException("Ya existe una marca con ese nombre");
        }

        return mapper.toResponseDto(repository.save(mapper.toEntity(dto)));
    }

    @Override
    @Transactional
    public ProductBrandResponseDto update(Long id, ProductBrandRequestDto dto) {

        ProductBrand existing = findBrandOrThrow(id);
        String name = dto.name().trim();

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("Ya existe otra marca con ese nombre");
        }

        existing.setName(name);

        return mapper.toResponseDto(repository.save(existing));
    }

    // No se desactiva una marca con modelos activos (409)
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        ProductBrand brand = findBrandOrThrow(id);

        if (Boolean.FALSE.equals(isActive) && modelRepository.existsByProductBrandIdAndIsActiveTrue(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar la marca porque tiene modelos activos"
            );
        }

        brand.setIsActive(isActive);

        repository.save(brand);
    }

    private ProductBrand findBrandOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la marca con ID: " + id
                ));
    }
}
