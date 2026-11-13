package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.role.RoleResponseDto;
import com.tamachihua_erp.api.repository.RoleRepository;
import com.tamachihua_erp.api.service.RoleService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponseDto> findAll() {

        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponseDto(
                        role.getId(),
                        role.getCode(),
                        role.getName()
                ))
                .toList();
    }
}
