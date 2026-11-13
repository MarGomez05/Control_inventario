package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.lot.LastCostItemDto;
import com.tamachihua_erp.api.dto.lot.LotCostResponseDto;
import com.tamachihua_erp.api.dto.lot.LotPresentationDto;
import com.tamachihua_erp.api.dto.lot.LotProductDto;
import com.tamachihua_erp.api.dto.lot.LotRequestDto;
import com.tamachihua_erp.api.dto.lot.LotResponseDto;
import com.tamachihua_erp.api.dto.lot.LotView;
import com.tamachihua_erp.api.dto.supplier.SupplierSummaryDto;
import com.tamachihua_erp.api.entity.ProductLot;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.entity.Supplier;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.repository.ProductLotRepository;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.SupplierRepository;
import com.tamachihua_erp.api.repository.UserRepository;
import com.tamachihua_erp.api.service.ProductLotService;
import com.tamachihua_erp.api.util.ExpirationSeverityCalculator;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLotServiceImpl implements ProductLotService {

    private final ProductLotRepository repository;
    private final ProductPresentationRepository productPresentationRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Override
    public Page<LotView> findAll(
            Long productId,
            Long supplierId,
            String invoiceNumber,
            Boolean onlyAvailable,
            Pageable pageable,
            boolean includeCosts
    ) {

        Specification<ProductLot> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (productId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("productPresentation").get("product").get("id"), productId)
            );
        }

        if (supplierId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("supplier").get("id"), supplierId)
            );
        }

        if (invoiceNumber != null && !invoiceNumber.isBlank()) {

            String pattern = "%" + invoiceNumber.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("invoiceNumber")), pattern)
            );
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {

            specification = specification.and((root, query, cb) ->
                    cb.greaterThan(root.get("availableQuantity"), BigDecimal.ZERO)
            );
        }

        return repository.findAll(specification, pageable)
                .map(lot -> toView(lot, includeCosts));
    }

    @Override
    public LotView findById(Long id, boolean includeCosts) {

        return toView(findLotOrThrow(id), includeCosts);
    }

    @Override
    @Transactional
    public LotView create(LotRequestDto dto, Long userId, boolean includeCosts) {

        ProductPresentation productPresentation = productPresentationRepository
                .findDetailedById(dto.productPresentationId())
                .orElseThrow(() -> new InvalidFieldException(
                        "productPresentationId",
                        "La presentación del producto no existe o está inactiva"
                ));

        if (!Boolean.TRUE.equals(productPresentation.getIsActive())) {
            throw new InvalidFieldException(
                    "productPresentationId",
                    "La presentación del producto no existe o está inactiva"
            );
        }

        Supplier supplier = supplierRepository.findById(dto.supplierId())
                .orElseThrow(() -> new InvalidFieldException(
                        "supplierId", "El proveedor no existe o está inactivo"
                ));

        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new InvalidFieldException(
                    "supplierId", "El proveedor no existe o está inactivo"
            );
        }

        String lotNumber = dto.lotNumber().trim();

        boolean lotTaken = repository
                .existsBySupplierIdAndLotNumberIgnoreCaseAndProductPresentationId(
                        supplier.getId(),
                        lotNumber,
                        productPresentation.getId()
                );

        if (lotTaken) {
            throw new ResourceConflictException(
                    "Ya existe un lote con ese número para el proveedor y la presentación"
            );
        }

        LocalDate entryDate = LocalDate.now();

        validateExpiration(
                dto.expirationDate(),
                entryDate,
                Boolean.TRUE.equals(productPresentation.getProduct().getRequiresExpiration())
        );

        ProductLot lot = new ProductLot();

        lot.setProductPresentation(productPresentation);
        lot.setSupplier(supplier);
        lot.setUser(userRepository.getReferenceById(userId));
        lot.setLotNumber(lotNumber);
        lot.setInvoiceNumber(dto.invoiceNumber().trim());
        lot.setInvoiceDate(dto.invoiceDate());
        lot.setEntryDate(entryDate);
        lot.setExpirationDate(dto.expirationDate());
        lot.setReceivedQuantity(dto.receivedQuantity());
        lot.setAvailableQuantity(dto.receivedQuantity());
        lot.setUnitCost(dto.unitCost());
        lot.setNotes(normalize(dto.notes()));

        ProductLot saved = repository.save(lot);

        return toView(saved, includeCosts);
    }

    @Override
    @Transactional
    public void updateAvailableQuantity(Long id, BigDecimal availableQuantity) {

        ProductLot lot = findLotOrThrow(id);

        if (availableQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFieldException(
                    "availableQuantity", "La cantidad disponible no puede ser negativa"
            );
        }

        if (availableQuantity.compareTo(lot.getReceivedQuantity()) > 0) {
            throw new InvalidFieldException(
                    "availableQuantity",
                    "La cantidad disponible no puede superar la cantidad recibida"
            );
        }

        lot.setAvailableQuantity(availableQuantity);

        repository.save(lot);
    }

    @Override
    public List<LastCostItemDto> findLastCosts(Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "No se encontró el producto con ID: " + productId
            );
        }

        List<ProductLot> lots = repository
                .findByProductPresentationProductIdOrderByEntryDateDescIdDesc(productId);

        Map<Long, ProductLot> latestByPresentation = new LinkedHashMap<>();

        for (ProductLot lot : lots) {
            latestByPresentation.putIfAbsent(
                    lot.getProductPresentation().getId(),
                    lot
            );
        }

        List<LastCostItemDto> result = new ArrayList<>();

        for (ProductLot lot : latestByPresentation.values()) {

            result.add(new LastCostItemDto(
                    lot.getProductPresentation().getId(),
                    lot.getProductPresentation().getPresentation().getName(),
                    lot.getUnitCost(),
                    lot.getId(),
                    lot.getEntryDate()
            ));
        }

        return result;
    }

    private void validateExpiration(
            LocalDate expirationDate,
            LocalDate entryDate,
            boolean requiresExpiration
    ) {

        if (expirationDate == null) {

            if (requiresExpiration) {
                throw new InvalidFieldException(
                        "expirationDate",
                        "La fecha de vencimiento es obligatoria para este producto"
                );
            }

            return;
        }

        if (!expirationDate.isAfter(entryDate)) {
            throw new InvalidFieldException(
                    "expirationDate",
                    "La fecha de vencimiento debe ser posterior a la fecha de ingreso"
            );
        }
    }

    private LotView toView(ProductLot lot, boolean includeCosts) {

        Long daysToExpire = lot.getExpirationDate() == null
                ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), lot.getExpirationDate());

        String severity = ExpirationSeverityCalculator.severity(daysToExpire);

        LotProductDto product = new LotProductDto(
                lot.getProductPresentation().getProduct().getId(),
                lot.getProductPresentation().getProduct().getCode(),
                lot.getProductPresentation().getProduct().getName()
        );

        LotPresentationDto presentation = new LotPresentationDto(
                lot.getProductPresentation().getId(),
                lot.getProductPresentation().getPresentation().getName()
        );

        SupplierSummaryDto supplier = new SupplierSummaryDto(
                lot.getSupplier().getId(),
                lot.getSupplier().getTradeName()
        );

        if (!includeCosts) {

            return new LotResponseDto(
                    lot.getId(),
                    lot.getLotNumber(),
                    product,
                    presentation,
                    supplier,
                    lot.getInvoiceNumber(),
                    lot.getInvoiceDate(),
                    lot.getEntryDate(),
                    lot.getExpirationDate(),
                    daysToExpire,
                    severity,
                    lot.getReceivedQuantity(),
                    lot.getAvailableQuantity(),
                    lot.getNotes(),
                    lot.getIsActive()
            );
        }

        BigDecimal totalCost = lot.getTotalCost() == null
                ? lot.getReceivedQuantity()
                        .multiply(lot.getUnitCost())
                        .setScale(2, RoundingMode.HALF_UP)
                : lot.getTotalCost();

        return new LotCostResponseDto(
                lot.getId(),
                lot.getLotNumber(),
                product,
                presentation,
                supplier,
                lot.getInvoiceNumber(),
                lot.getInvoiceDate(),
                lot.getEntryDate(),
                lot.getExpirationDate(),
                daysToExpire,
                severity,
                lot.getReceivedQuantity(),
                lot.getAvailableQuantity(),
                lot.getNotes(),
                lot.getIsActive(),
                lot.getUnitCost(),
                totalCost
        );
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private ProductLot findLotOrThrow(Long id) {

        return repository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el lote con ID: " + id
                ));
    }
}
