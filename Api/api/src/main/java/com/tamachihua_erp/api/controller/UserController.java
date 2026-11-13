package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.common.PageResponseDto;
import com.tamachihua_erp.api.dto.common.StatusRequestDto;
import com.tamachihua_erp.api.dto.user.UserCreateRequestDto;
import com.tamachihua_erp.api.dto.user.UserResponseDto;
import com.tamachihua_erp.api.dto.user.UserUpdateRequestDto;
import com.tamachihua_erp.api.service.UserService;

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

// Gestión de usuarios del panel (HU-02, contrato 3). Solo ADMIN.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    public ResponseEntity<PageResponseDto<UserResponseDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable limitedPageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        return ResponseEntity.ok(PageResponseDto.from(
                service.findAll(search, roleId, isActive, limitedPageable)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> create(
            @Valid @RequestBody UserCreateRequestDto dto
    ) {
        UserResponseDto created = service.create(dto);

        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.update(id, dto, currentUserId(jwt)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.changeStatus(id, dto.isActive(), currentUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Jwt jwt) {

        Number userId = jwt.getClaim("userId");

        if (userId == null) {
            throw new BadCredentialsException("Token inválido");
        }

        return userId.longValue();
    }
}
