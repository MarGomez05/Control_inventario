package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.auth.LoginRequestDto;
import com.tamachihua_erp.api.dto.auth.LoginResponseDto;
import com.tamachihua_erp.api.dto.auth.UserAuthResponseDto;
import com.tamachihua_erp.api.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            @RequestHeader(
                    value = "X-Client-Interface",
                    required = false
            ) String clientInterface
    ) {
        return ResponseEntity.ok(
                authService.login(request, clientInterface)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserAuthResponseDto> me(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Number userId = jwt.getClaim("userId");

        List<String> roles = jwt.getClaimAsStringList("roles");

        if (userId == null || roles == null || roles.size() != 1
                || roles.get(0).isBlank()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Token inválido"
            );
        }

        UserAuthResponseDto response = new UserAuthResponseDto(
                userId.longValue(),
                jwt.getSubject(),
                jwt.getClaimAsString("fullName"),
                roles.get(0)
        );

        return ResponseEntity.ok(response);
    }
}