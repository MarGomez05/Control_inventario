package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.user.UserCreateRequestDto;
import com.tamachihua_erp.api.dto.user.UserResponseDto;
import com.tamachihua_erp.api.dto.user.UserUpdateRequestDto;
import com.tamachihua_erp.api.entity.Role;
import com.tamachihua_erp.api.entity.User;
import com.tamachihua_erp.api.exception.InvalidFieldException;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.UserMapper;
import com.tamachihua_erp.api.repository.RoleRepository;
import com.tamachihua_erp.api.repository.UserRepository;
import com.tamachihua_erp.api.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    @Override
    public Page<UserResponseDto> findAll(
            String search,
            Long roleId,
            Boolean isActive,
            Pageable pageable
    ) {

        Specification<User> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("username")), pattern),
                            cb.like(cb.lower(root.get("fullName")), pattern),
                            cb.like(cb.lower(root.get("email")), pattern)
                    )
            );
        }

        if (roleId != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("role").get("id"), roleId)
            );
        }

        if (isActive != null) {

            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive)
            );
        }

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    public UserResponseDto findById(Long id) {

        return mapper.toResponseDto(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponseDto create(UserCreateRequestDto dto) {

        String username = dto.username().trim();

        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ResourceConflictException("Ya existe un usuario con ese nombre de usuario");
        }

        String email = normalizeEmail(dto.email());

        if (email != null && repository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Ya existe un usuario con ese correo");
        }

        User user = new User();

        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setFullName(dto.fullName().trim());
        user.setEmail(email);
        user.setRole(findAssignableRole(dto.roleId()));

        return mapper.toResponseDto(repository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto update(
            Long id,
            UserUpdateRequestDto dto,
            Long currentUserId
    ) {

        User existing = findUserOrThrow(id);

        String email = normalizeEmail(dto.email());

        if (email != null && repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResourceConflictException("Ya existe otro usuario con ese correo");
        }

        Role role = findAssignableRole(dto.roleId());

        // Si el último ADMIN activo cambia de rol, el sistema quedaría sin administrador
        if (isActiveAdmin(existing)
                && !ADMIN_ROLE.equals(role.getCode())
                && repository.countByRoleCodeAndIsActiveTrue(ADMIN_ROLE) <= 1) {

            throw new ResourceConflictException(
                    "No se puede quitar el rol ADMIN al último administrador activo"
            );
        }

        existing.setFullName(dto.fullName().trim());
        existing.setEmail(email);
        existing.setRole(role);

        return mapper.toResponseDto(repository.save(existing));
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive, Long currentUserId) {

        User user = findUserOrThrow(id);

        if (Boolean.FALSE.equals(isActive)) {

            if (Objects.equals(id, currentUserId)) {
                throw new ResourceConflictException(
                        "Un administrador no puede desactivar su propio usuario"
                );
            }

            if (isActiveAdmin(user)
                    && repository.countByRoleCodeAndIsActiveTrue(ADMIN_ROLE) <= 1) {

                throw new ResourceConflictException(
                        "No se puede desactivar al último administrador activo"
                );
            }
        }

        user.setIsActive(isActive);

        repository.save(user);
    }

    private boolean isActiveAdmin(User user) {

        return Boolean.TRUE.equals(user.getIsActive())
                && ADMIN_ROLE.equals(user.getRole().getCode());
    }

    private Role findAssignableRole(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidFieldException(
                        "roleId", "El rol no existe o está inactivo"
                ));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new InvalidFieldException("roleId", "El rol no existe o está inactivo");
        }

        return role;
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

    private User findUserOrThrow(Long id) {

        return repository.findWithRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con ID: " + id
                ));
    }
}
