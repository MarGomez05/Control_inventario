package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.role.RoleSummaryDto;
import com.tamachihua_erp.api.dto.user.UserResponseDto;
import com.tamachihua_erp.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {

        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                new RoleSummaryDto(
                        user.getRole().getId(),
                        user.getRole().getName()
                ),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
