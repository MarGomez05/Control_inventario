package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.lot.AvailableQuantityRequestDto;
import com.tamachihua_erp.api.dto.lot.LotCostResponseDto;
import com.tamachihua_erp.api.dto.lot.LotRequestDto;
import com.tamachihua_erp.api.dto.lot.LotResponseDto;
import com.tamachihua_erp.api.dto.lot.LotView;
import com.tamachihua_erp.api.service.ProductLotService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Registro y consulta de lotes (HU-10, HU-11, contrato 7)
@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class ProductLotController {

    private final ProductLotService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<LotView>> findAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) Boolean onlyAvailable,
            @PageableDefault(page = 0, size = 20, sort = "expirationDate",
                    direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.findAll(
                        productId,
                        supplierId,
                        invoiceNumber,
                        onlyAvailable,
                        limitedPageable,
                        canSeeCosts(jwt)
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LotView> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.findById(id, canSeeCosts(jwt)));
    }

    @PostMapping
    public ResponseEntity<LotView> create(
            @Valid @RequestBody LotRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        LotView created = service.create(dto, currentUserId(jwt), canSeeCosts(jwt));

        return ResponseEntity
                .created(URI.create("/api/lots/" + lotId(created)))
                .body(created);
    }

    @PatchMapping("/{id}/available-quantity")
    public ResponseEntity<Void> updateAvailableQuantity(
            @PathVariable Long id,
            @Valid @RequestBody AvailableQuantityRequestDto dto
    ) {
        service.updateAvailableQuantity(id, dto.availableQuantity());
        return ResponseEntity.noContent().build();
    }

    // unitCost y totalCost solo se devuelven para ADMIN y PURCHASING (contrato 7)
    private boolean canSeeCosts(Jwt jwt) {

        List<String> roles = jwt.getClaimAsStringList("roles");

        return roles != null
                && (roles.contains("ADMIN") || roles.contains("PURCHASING"));
    }

    private Long currentUserId(Jwt jwt) {

        Number userId = jwt.getClaim("userId");

        if (userId == null) {
            throw new BadCredentialsException("Token inválido");
        }

        return userId.longValue();
    }

    private Long lotId(LotView lot) {

        if (lot instanceof LotCostResponseDto cost) {
            return cost.id();
        }

        return ((LotResponseDto) lot).id();
    }
}
