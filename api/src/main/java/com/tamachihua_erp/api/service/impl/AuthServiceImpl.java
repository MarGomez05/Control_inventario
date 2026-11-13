package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.auth.*;
import com.tamachihua_erp.api.entity.User;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.repository.UserRepository;
import com.tamachihua_erp.api.security.JwtService;
import com.tamachihua_erp.api.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    // Roles que pueden entrar a cada interfaz (contrato 3).
    private static final Set<String> APP_ROLES = Set.of(
            "ADMIN",
            "WAREHOUSE",
            "SELLER"
    );

    private static final Set<String> PANEL_ROLES = Set.of(
            "ADMIN",
            "WAREHOUSE",
            "PURCHASING"
    );

    private static final String APP_INTERFACE = "app";
    private static final String PANEL_INTERFACE = "panel";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration}")
    private long expirationSeconds;

    @Override
    public LoginResponseDto login(
            LoginRequestDto request,
            String clientInterface
    ) {

        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(),
                            request.password()
                    )
            );

        } catch (AuthenticationException exception) {

            throw new BadCredentialsException(
                    "Credenciales inválidas"
            );
        }

        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmailIgnoreCase(request.username()))
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado")
                );

        validateRoleForInterface(user, clientInterface);

        String token = jwtService.generateToken(user);

        UserAuthResponseDto userResponse =
                new UserAuthResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRole().getCode()
                );

        return new LoginResponseDto(
                token,
                "Bearer",
                expirationSeconds,
                userResponse
        );
    }

    // La app y el panel comparten el login, pero no aceptan los mismos roles.
    // El cliente indica su interfaz con el encabezado X-Client-Interface.
    private void validateRoleForInterface(
            User user,
            String clientInterface
    ) {
        if (clientInterface == null || clientInterface.isBlank()) {
            // Clientes que no indican interfaz (por ejemplo, pruebas de API):
            // se mantiene el comportamiento sin restricción adicional.
            return;
        }

        String role = user.getRole().getCode();

        switch (clientInterface.trim().toLowerCase(Locale.ROOT)) {
            case APP_INTERFACE -> {
                if (!APP_ROLES.contains(role)) {
                    throw new AccessDeniedException(
                            "Tu rol no tiene permiso para ingresar a la aplicación móvil"
                    );
                }
            }

            case PANEL_INTERFACE -> {
                if (!PANEL_ROLES.contains(role)) {
                    throw new AccessDeniedException(
                            "Tu rol no tiene permiso para ingresar al panel administrativo"
                    );
                }
            }

            default -> throw new InvalidFieldException(
                    "X-Client-Interface",
                    "La interfaz debe ser 'app' o 'panel'"
            );
        }
    }
}