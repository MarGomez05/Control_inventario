package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.product.ProductDetailDto;
import com.tamachihua_erp.api.dto.product.ProductListItemDto;
import com.tamachihua_erp.api.dto.product.ProductRequestDto;
import com.tamachihua_erp.api.dto.product.StockByPresentationDto;
import com.tamachihua_erp.api.entity.Product;
import com.tamachihua_erp.api.entity.ProductCategory;
import com.tamachihua_erp.api.entity.ProductModel;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.entity.ProductType;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductMapper;
import com.tamachihua_erp.api.mapper.ProductPresentationMapper;
import com.tamachihua_erp.api.repository.ProductCategoryRepository;
import com.tamachihua_erp.api.repository.ProductLotRepository;
import com.tamachihua_erp.api.repository.ProductModelRepository;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.ProductTypeRepository;
import com.tamachihua_erp.api.repository.projection.PresentationStockProjection;
import com.tamachihua_erp.api.service.ProductPresentationService;
import com.tamachihua_erp.api.service.ProductService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductRepository repository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductModelRepository productModelRepository;
    private final ProductPresentationRepository productPresentationRepository;
    private final ProductLotRepository productLotRepository;
    private final ProductMapper mapper;
    private final ProductPresentationMapper presentationMapper;
    private final ProductPresentationService productPresentationService;

    // Listado paginado con filtros: search (nombre o código), categoría, tipo e isActive
    @Override
    public Page<ProductListItemDto> findAll(
            String search,
            Long categoryId,
            Long productTypeId,
            Boolean isActive,
            Pageable pageable
    ) {

        Specification<Product> specification =
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

        if (categoryId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("productCategory").get("id"), categoryId)
            );
        }

        if (productTypeId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("productType").get("id"), productTypeId)
            );
        }

        if (isActive != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive)
            );
        }

        Page<Product> page = repository.findAll(specification, pageable);

        List<Long> productIds = page.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<StockByPresentationDto>> stockByProduct =
                loadStockByProduct(productIds);

        return page.map(product -> mapper.toListItem(
                product,
                stockByProduct.getOrDefault(product.getId(), List.of())
        ));
    }

    // Detalle del producto con todas sus presentaciones (activas e inactivas, para el panel)
    @Override
    public ProductDetailDto findById(Long id) {

        Product product = repository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto con ID: " + id
                ));

        return mapper.toDetailDto(
                product,
                productPresentationService.findByProductId(id)
        );
    }

    @Override
    @Transactional
    public ProductDetailDto create(ProductRequestDto dto) {

        String code = dto.code().trim();

        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ResourceConflictException("Ya existe un producto con ese código");
        }

        Product product = new Product();

        applyChanges(product, dto, code, null, null, null);

        Product saved = repository.save(product);

        return findById(saved.getId());
    }

    @Override
    @Transactional
    public ProductDetailDto update(Long id, ProductRequestDto dto) {

        Product existing = findProductOrThrow(id);

        String code = dto.code().trim();

        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResourceConflictException("Ya existe otro producto con ese código");
        }

        // D-01: no se permite quitar el control de vencimiento si ya tiene lotes activos
        if (Boolean.FALSE.equals(dto.requiresExpiration())
                && productLotRepository.countActiveLotsByProductId(id) > 0) {
            throw new ResourceConflictException(
                    "No se puede cambiar el producto a que no requiere vencimiento porque tiene lotes activos"
            );
        }

        applyChanges(
                existing,
                dto,
                code,
                existing.getProductCategory(),
                existing.getProductType(),
                existing.getProductModel()
        );

        repository.save(existing);

        return findById(id);
    }

    // Desactivar siempre se permite. Activar exige que categoría, tipo y modelo estén activos.
    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        Product product = findProductOrThrow(id);

        if (Boolean.TRUE.equals(isActive)) {

            if (!Boolean.TRUE.equals(product.getProductCategory().getIsActive())) {
                throw new ResourceConflictException(
                        "No se puede activar el producto porque su categoría está inactiva"
                );
            }

            if (!Boolean.TRUE.equals(product.getProductType().getIsActive())) {
                throw new ResourceConflictException(
                        "No se puede activar el producto porque su tipo está inactivo"
                );
            }

            if (product.getProductModel() != null
                    && !Boolean.TRUE.equals(product.getProductModel().getIsActive())) {
                throw new ResourceConflictException(
                        "No se puede activar el producto porque su modelo está inactivo"
                );
            }
        }

        product.setIsActive(isActive);

        repository.save(product);
    }

    // Agrupa la existencia de las presentaciones activas por producto
    private Map<Long, List<StockByPresentationDto>> loadStockByProduct(List<Long> productIds) {

        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductPresentation> presentations = productPresentationRepository
                .findByProductIdInAndIsActiveTrueOrderByPresentationNameAsc(productIds);

        List<Long> presentationIds = presentations.stream()
                .map(ProductPresentation::getId)
                .toList();

        Map<Long, BigDecimal> stockByPresentation = loadStock(presentationIds);

        Map<Long, List<StockByPresentationDto>> result = new LinkedHashMap<>();

        for (ProductPresentation presentation : presentations) {

            Long productId = presentation.getProduct().getId();

            result.computeIfAbsent(productId, key -> new ArrayList<>())
                    .add(presentationMapper.toStockDto(
                            presentation,
                            stockByPresentation.getOrDefault(presentation.getId(), ZERO)
                    ));
        }

        return result;
    }

    private Map<Long, BigDecimal> loadStock(List<Long> presentationIds) {

        if (presentationIds.isEmpty()) {
            return Map.of();
        }

        return productLotRepository
                .sumStockByProductPresentationIds(presentationIds)
                .stream()
                .collect(Collectors.toMap(
                        PresentationStockProjection::getProductPresentationId,
                        projection -> projection.getStock() == null
                                ? ZERO
                                : projection.getStock()
                ));
    }

    private void applyChanges(
            Product product,
            ProductRequestDto dto,
            String code,
            ProductCategory currentCategory,
            ProductType currentType,
            ProductModel currentModel
    ) {

        product.setCode(code);
        product.setName(dto.name().trim());
        product.setDescription(dto.description());
        product.setRequiresExpiration(dto.requiresExpiration());
        product.setProductCategory(findAssignableCategory(dto.categoryId(), currentCategory));
        product.setProductType(findAssignableType(dto.productTypeId(), currentType));
        product.setProductModel(findAssignableModel(dto.productModelId(), currentModel));
    }

    // La categoría debe existir y estar activa (contrato 5.1). Al editar se acepta la que ya tenía.
    private ProductCategory findAssignableCategory(Long categoryId, ProductCategory current) {

        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidFieldException(
                        "categoryId", "La categoría no existe o está inactiva"
                ));

        boolean sameAsCurrent = current != null
                && Objects.equals(current.getId(), category.getId());

        if (!sameAsCurrent && !Boolean.TRUE.equals(category.getIsActive())) {
            throw new InvalidFieldException(
                    "categoryId", "La categoría no existe o está inactiva"
            );
        }

        return category;
    }

    // El tipo debe existir y estar activo (contrato 5.1). Al editar se acepta el que ya tenía.
    private ProductType findAssignableType(Long productTypeId, ProductType current) {

        ProductType type = productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> new InvalidFieldException(
                        "productTypeId", "El tipo de producto no existe o está inactivo"
                ));

        boolean sameAsCurrent = current != null
                && Objects.equals(current.getId(), type.getId());

        if (!sameAsCurrent && !Boolean.TRUE.equals(type.getIsActive())) {
            throw new InvalidFieldException(
                    "productTypeId", "El tipo de producto no existe o está inactivo"
            );
        }

        return type;
    }

    // El modelo es opcional; si viene, debe existir y estar activo.
    private ProductModel findAssignableModel(Long productModelId, ProductModel current) {

        if (productModelId == null) {
            return null;
        }

        ProductModel model = productModelRepository.findById(productModelId)
                .orElseThrow(() -> new InvalidFieldException(
                        "productModelId", "El modelo no existe o está inactivo"
                ));

        boolean sameAsCurrent = current != null
                && Objects.equals(current.getId(), model.getId());

        if (!sameAsCurrent && !Boolean.TRUE.equals(model.getIsActive())) {
            throw new InvalidFieldException(
                    "productModelId", "El modelo no existe o está inactivo"
            );
        }

        return model;
    }

    private Product findProductOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto con ID: " + id
                ));
    }
}
