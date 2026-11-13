package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.productprice.PriceRequestDto;
import com.tamachihua_erp.api.dto.productprice.ProductPriceResponseDto;
import com.tamachihua_erp.api.service.ProductPriceService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Cambio de precio e historial por presentación del producto (HU-08, contrato 5.3)
@RestController
@RequestMapping("/api/products/{productId}/presentations/{presentationId}/prices")
@RequiredArgsConstructor
public class ProductPriceController {

    private final ProductPriceService service;

    @PostMapping
    public ResponseEntity<ProductPriceResponseDto> changePrice(
            @PathVariable Long productId,
            @PathVariable Long presentationId,
            @Valid @RequestBody PriceRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProductPriceResponseDto created = service.changePrice(
                productId,
                presentationId,
                dto,
                currentUserId(jwt)
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/products/" + productId
                                + "/presentations/" + presentationId
                                + "/prices/" + created.id()
                ))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProductPriceResponseDto>> findHistory(
            @PathVariable Long productId,
            @PathVariable Long presentationId
    ) {
        return ResponseEntity.ok(service.findHistory(productId, presentationId));
    }

    // El usuario se toma siempre del token, nunca del cuerpo (contrato 1)
    private Long currentUserId(Jwt jwt) {

        Number userId = jwt.getClaim("userId");

        if (userId == null) {
            throw new BadCredentialsException("Token inválido");
        }

        return userId.longValue();
    }
}
