package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.app.AppProductDetailDto;
import com.tamachihua_erp.api.dto.app.AppProductPresentationDto;
import com.tamachihua_erp.api.dto.app.AppProductSummaryDto;
import com.tamachihua_erp.api.dto.app.AppStockDto;
import com.tamachihua_erp.api.entity.Product;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.entity.ProductPrice;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.repository.ProductLotRepository;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductPriceRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.projection.PresentationStockProjection;
import com.tamachihua_erp.api.service.AppProductService;
import com.tamachihua_erp.api.util.StockStatusCalculator;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppProductServiceImpl implements AppProductService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductRepository repository;
    private final ProductPresentationRepository productPresentationRepository;
    private final ProductPriceRepository productPriceRepository;
    private final ProductLotRepository productLotRepository;

    // Búsqueda por nombre, código de producto o código exacto de presentación.
    // Solo productos y presentaciones activos (contrato 10, HU-13).
    @Override
    public Page<AppProductSummaryDto> search(
            String search,
            Long categoryId,
            Long productTypeId,
            Pageable pageable
    ) {

        Specification<Product> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("isActive"));

        if (search != null && !search.isBlank()) {

            String term = search.trim();
            String pattern = "%" + term.toLowerCase() + "%";

            List<Long> productIdsByPresentationCode =
                    productPresentationRepository.findProductIdsByPresentationCode(term);

            specification = specification.and((root, query, cb) -> {

                var byText = cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)
                );

                if (productIdsByPresentationCode.isEmpty()) {
                    return byText;
                }

                return cb.or(byText, root.get("id").in(productIdsByPresentationCode));
            });
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

        Page<Product> page = repository.findAll(specification, pageable);

        List<Long> productIds = page.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<AppStockDto>> stockByProduct = loadStockByProduct(productIds);

        return page.map(product -> new AppProductSummaryDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getProductCategory().getName(),
                stockByProduct.getOrDefault(product.getId(), List.of())
        ));
    }

    // Detalle de un producto activo con sus presentaciones activas (HU-14)
    @Override
    public AppProductDetailDto findById(Long id) {

        Product product = repository.findDetailedById(id)
                .filter(found -> Boolean.TRUE.equals(found.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto con ID: " + id
                ));

        List<ProductPresentation> presentations = productPresentationRepository
                .findByProductIdAndIsActiveTrueOrderByPresentationNameAsc(id);

        List<Long> presentationIds = presentations.stream()
                .map(ProductPresentation::getId)
                .toList();

        Map<Long, BigDecimal> stockByPresentation = loadStock(presentationIds);
        Map<Long, BigDecimal> currentPrices = loadCurrentPrices(presentationIds);

        List<AppProductPresentationDto> presentationDtos = presentations.stream()
                .map(presentation -> {

                    BigDecimal stock = stockByPresentation
                            .getOrDefault(presentation.getId(), ZERO);

                    return new AppProductPresentationDto(
                            presentation.getCode(),
                            presentation.getPresentation().getName(),
                            presentation.getPresentation().getMeasureUnit().getName(),
                            presentation.getPresentation().getQuantity(),
                            currentPrices.get(presentation.getId()),
                            stock,
                            StockStatusCalculator.calculate(stock, presentation.getMinStock())
                    );
                })
                .toList();

        String brandName = product.getProductModel() == null
                ? null
                : product.getProductModel().getProductBrand().getName();

        return new AppProductDetailDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getProductCategory().getName(),
                product.getProductType().getName(),
                brandName,
                presentationDtos
        );
    }

    private Map<Long, List<AppStockDto>> loadStockByProduct(List<Long> productIds) {

        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductPresentation> presentations = productPresentationRepository
                .findByProductIdInAndIsActiveTrueOrderByPresentationNameAsc(productIds);

        List<Long> presentationIds = presentations.stream()
                .map(ProductPresentation::getId)
                .toList();

        Map<Long, BigDecimal> stockByPresentation = loadStock(presentationIds);

        Map<Long, List<AppStockDto>> result = new LinkedHashMap<>();

        for (ProductPresentation presentation : presentations) {

            BigDecimal stock = stockByPresentation
                    .getOrDefault(presentation.getId(), ZERO);

            result.computeIfAbsent(presentation.getProduct().getId(), key -> new ArrayList<>())
                    .add(new AppStockDto(
                            presentation.getPresentation().getName(),
                            stock,
                            StockStatusCalculator.calculate(stock, presentation.getMinStock())
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
}
