package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationRequestDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationUpdateRequestDto;
import com.tamachihua_erp.api.entity.Presentation;
import com.tamachihua_erp.api.entity.Product;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.entity.ProductPrice;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductPresentationMapper;
import com.tamachihua_erp.api.repository.PresentationRepository;
import com.tamachihua_erp.api.repository.ProductLotRepository;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductPriceRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.projection.PresentationStockProjection;
import com.tamachihua_erp.api.service.ProductPresentationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPresentationServiceImpl implements ProductPresentationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductPresentationRepository repository;
    private final ProductRepository productRepository;
    private final PresentationRepository presentationRepository;
    private final ProductPriceRepository productPriceRepository;
    private final ProductLotRepository productLotRepository;
    private final ProductPresentationMapper mapper;

    // Presentaciones del producto con existencia, mínimo y precio vigente
    @Override
    public List<ProductPresentationResponseDto> findByProductId(Long productId) {

        findProductOrThrow(productId);

        List<ProductPresentation> presentations = repository
                .findByProductIdOrderByPresentationNameAsc(productId);

        List<Long> presentationIds = presentations.stream()
                .map(ProductPresentation::getId)
                .toList();

        Map<Long, BigDecimal> stockByPresentation = loadStock(presentationIds);
        Map<Long, BigDecimal> currentPrices = loadCurrentPrices(presentationIds);

        return presentations.stream()
                .map(presentation -> mapper.toResponseDto(
                        presentation,
                        stockByPresentation.getOrDefault(presentation.getId(), ZERO),
                        currentPrices.get(presentation.getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProductPresentationResponseDto create(
            Long productId,
            ProductPresentationRequestDto dto
    ) {

        Product product = findProductOrThrow(productId);

        Presentation presentation = findAssignablePresentation(dto.presentationId());

        String code = dto.code().trim();

        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ResourceConflictException(
                    "Ya existe una presentación de producto con ese código"
            );
        }

        if (repository.existsByProductIdAndPresentationId(productId, presentation.getId())) {
            throw new ResourceConflictException(
                    "El producto ya tiene asignada esa presentación"
            );
        }

        ProductPresentation productPresentation = new ProductPresentation();

        productPresentation.setProduct(product);
        productPresentation.setPresentation(presentation);
        productPresentation.setCode(code);
        productPresentation.setMinStock(dto.minStock());

        ProductPresentation saved = repository.save(productPresentation);

        return mapper.toResponseDto(saved, ZERO, null);
    }

    @Override
    @Transactional
    public ProductPresentationResponseDto update(
            Long productId,
            Long id,
            ProductPresentationUpdateRequestDto dto
    ) {

        findProductOrThrow(productId);

        ProductPresentation productPresentation = findPresentationOrThrow(productId, id);

        String code = dto.code().trim();

        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResourceConflictException(
                    "Ya existe otra presentación de producto con ese código"
            );
        }

        productPresentation.setCode(code);
        productPresentation.setMinStock(dto.minStock());

        ProductPresentation saved = repository.save(productPresentation);

        BigDecimal stock = loadStock(List.of(id)).getOrDefault(id, ZERO);
        BigDecimal currentPrice = loadCurrentPrices(List.of(id)).get(id);

        return mapper.toResponseDto(saved, stock, currentPrice);
    }

    // Desactivar siempre se permite. Activar exige que la presentación base esté activa.
    @Override
    @Transactional
    public void changeStatus(Long productId, Long id, Boolean isActive) {

        findProductOrThrow(productId);

        ProductPresentation productPresentation = findPresentationOrThrow(productId, id);

        if (Boolean.TRUE.equals(isActive)
                && !Boolean.TRUE.equals(productPresentation.getPresentation().getIsActive())) {
            throw new ResourceConflictException(
                    "No se puede activar la presentación del producto porque su presentación está inactiva"
            );
        }

        productPresentation.setIsActive(isActive);

        repository.save(productPresentation);
    }

    // La presentación base debe existir y estar activa (contrato 5.2)
    private Presentation findAssignablePresentation(Long presentationId) {

        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new InvalidFieldException(
                        "presentationId", "La presentación no existe o está inactiva"
                ));

        if (!Boolean.TRUE.equals(presentation.getIsActive())) {
            throw new InvalidFieldException(
                    "presentationId", "La presentación no existe o está inactiva"
            );
        }

        return presentation;
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

    private Map<Long, BigDecimal> loadCurrentPrices(List<Long> presentationIds) {

        if (presentationIds.isEmpty()) {
            return Map.of();
        }

        return productPriceRepository
                .findByProductPresentationIdInAndEndDateIsNull(presentationIds)
                .stream()
                .collect(Collectors.toMap(
                        price -> price.getProductPresentation().getId(),
                        ProductPrice::getPrice
                ));
    }

    private Product findProductOrThrow(Long productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto con ID: " + productId
                ));
    }

    private ProductPresentation findPresentationOrThrow(Long productId, Long id) {

        return repository.findByProductIdAndId(productId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la presentación del producto con ID: " + id
                ));
    }
}
