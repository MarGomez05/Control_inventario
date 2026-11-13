package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.productprice.PriceRequestDto;
import com.tamachihua_erp.api.dto.productprice.ProductPriceResponseDto;
import com.tamachihua_erp.api.entity.ProductPrice;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.ProductPriceMapper;
import com.tamachihua_erp.api.repository.ProductPresentationRepository;
import com.tamachihua_erp.api.repository.ProductPriceRepository;
import com.tamachihua_erp.api.repository.ProductRepository;
import com.tamachihua_erp.api.repository.UserRepository;
import com.tamachihua_erp.api.service.ProductPriceService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPriceServiceImpl implements ProductPriceService {

    private final ProductPriceRepository repository;
    private final ProductPresentationRepository productPresentationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductPriceMapper mapper;

    // Cierra el precio vigente y registra el nuevo en una sola transacción (contrato 5.3)
    @Override
    @Transactional
    public ProductPriceResponseDto changePrice(
            Long productId,
            Long presentationId,
            PriceRequestDto dto,
            Long userId
    ) {

        // Bloquea la fila de la presentación para serializar cambios concurrentes
        ProductPresentation productPresentation =
                productPresentationRepository.findByIdForUpdate(presentationId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No se encontró la presentación del producto con ID: "
                                        + presentationId
                        ));

        if (!productPresentation.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(
                    "No se encontró la presentación del producto con ID: " + presentationId
            );
        }

        LocalDateTime now = LocalDateTime.now();

        repository.findByProductPresentationIdAndEndDateIsNull(presentationId)
                .ifPresent(currentPrice -> {
                    currentPrice.setEndDate(now);
                    // Se fuerza el UPDATE antes de insertar el nuevo precio: el índice único
                    // parcial (una sola fila con end_date NULL) se evalúa en cada sentencia.
                    repository.saveAndFlush(currentPrice);
                });

        ProductPrice newPrice = new ProductPrice();

        newPrice.setProductPresentation(productPresentation);
        newPrice.setUser(userRepository.getReferenceById(userId));
        newPrice.setPrice(dto.price().setScale(2, RoundingMode.HALF_UP));
        newPrice.setStartDate(now);
        newPrice.setEndDate(null);

        return mapper.toResponseDto(repository.save(newPrice));
    }

    @Override
    public List<ProductPriceResponseDto> findHistory(Long productId, Long presentationId) {

        findProductOrThrow(productId);

        productPresentationRepository.findByProductIdAndId(productId, presentationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la presentación del producto con ID: " + presentationId
                ));

        return repository.findByProductPresentationIdOrderByStartDateDesc(presentationId)
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    private void findProductOrThrow(Long productId) {

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto con ID: " + productId
                ));
    }
}
