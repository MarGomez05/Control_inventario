package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.model.ProductModelRequestDto;
import com.tamachihua_erp.api.dto.model.ProductModelResponseDto;
import com.tamachihua_erp.api.entity.ProductBrand;
import com.tamachihua_erp.api.entity.ProductModel;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductModelMapper;
import com.tamachihua_erp.api.repository.ProductBrandRepository;
import com.tamachihua_erp.api.repository.ProductModelRepository;
import com.tamachihua_erp.api.service.ProductModelService;

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
public class ProductModelServiceImpl implements ProductModelService {

    private final ProductModelRepository repository;
    private final ProductBrandRepository brandRepository;
    private final ProductModelMapper mapper;

    // Listado paginado con filtros: search (nombre del modelo o de la marca), isActive y brandId
    @Override
    public Page<ProductModelResponseDto> findAll(
            String search,
            Boolean isActive,
            Long brandId,
            Pageable pageable
    ) {

        Specification<ProductModel> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("productBrand").get("name")), pattern)
                    )
            );
        }

        if (isActive != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive)
            );
        }

        if (brandId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("productBrand").get("id"), brandId)
            );
        }

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }

    // Modelos activos de marcas activas; si llega brandId, solo los de esa marca
    @Override
    public List<ProductModelResponseDto> findActiveOptions(Long brandId) {

        List<ProductModel> models = brandId == null
                ? repository.findAllByIsActiveTrueAndProductBrandIsActiveTrueOrderByNameAsc()
                : repository.findAllByProductBrandIdAndIsActiveTrueAndProductBrandIsActiveTrueOrderByNameAsc(brandId);

        return models.stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public ProductModelResponseDto findById(Long id) {

        return mapper.toResponseDto(findModelOrThrow(id));
    }

    @Override
    @Transactional
    public ProductModelResponseDto create(ProductModelRequestDto dto) {

        ProductBrand brand = findAssignableBrand(dto.brandId(), null);
        String name = dto.name().trim();

        if (repository.existsByProductBrandIdAndNameIgnoreCase(brand.getId(), name)) {
            throw new ResourceConflictException("Ya existe un modelo con ese nombre en la marca " + brand.getName());
        }

        ProductModel model = new ProductModel();
        model.setProductBrand(brand);
        model.setName(name);

        return mapper.toResponseDto(repository.save(model));
    }

    @Override
    @Transactional
    public ProductModelResponseDto update(Long id, ProductModelRequestDto dto) {

        ProductModel existing = findModelOrThrow(id);
        ProductBrand brand = findAssignableBrand(dto.brandId(), existing.getProductBrand());
        String name = dto.name().trim();

        if (repository.existsByProductBrandIdAndNameIgnoreCaseAndIdNot(brand.getId(), name, id)) {
            throw new ResourceConflictException("Ya existe otro modelo con ese nombre en la marca " + brand.getName());
        }

        existing.setProductBrand(brand);
        existing.setName(name);

        return mapper.toResponseDto(repository.save(existing));
    }

    // Desactivar: no si lo usan productos activos. Activar: no si su marca está inactiva.
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        ProductModel model = findModelOrThrow(id);

        if (Boolean.FALSE.equals(isActive) && repository.isUsedByActiveProducts(id)) {
            throw new ResourceConflictException(
                    "No se puede desactivar el modelo porque lo usan productos activos"
            );
        }

        if (Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(model.getProductBrand().getIsActive())) {
            throw new ResourceConflictException(
                    "No se puede activar el modelo porque su marca está inactiva"
            );
        }

        model.setIsActive(isActive);

        repository.save(model);
    }

    // La marca debe existir y estar activa (contrato 4.4). Al editar se acepta la marca que ya tenía.
    private ProductBrand findAssignableBrand(Long brandId, ProductBrand current) {

        ProductBrand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new InvalidFieldException(
                        "brandId", "La marca no existe o está inactiva"
                ));

        boolean sameAsCurrent = current != null && Objects.equals(current.getId(), brand.getId());

        if (!sameAsCurrent && !Boolean.TRUE.equals(brand.getIsActive())) {
            throw new InvalidFieldException("brandId", "La marca no existe o está inactiva");
        }

        return brand;
    }

    private ProductModel findModelOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el modelo con ID: " + id
                ));
    }
}
