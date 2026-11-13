package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.user.UserCreateRequestDto;
import com.tamachihua_erp.api.dto.user.UserResponseDto;
import com.tamachihua_erp.api.dto.user.UserUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Gestión de usuarios del panel (HU-02)
public interface UserService {

    Page<UserResponseDto> findAll(
            String search,
            Long roleId,
            Boolean isActive,
            Pageable pageable
    );

    UserResponseDto findById(Long id);

    UserResponseDto create(UserCreateRequestDto dto);

    UserResponseDto update(Long id, UserUpdateRequestDto dto, Long currentUserId);

    void changeStatus(Long id, Boolean isActive, Long currentUserId);
}
