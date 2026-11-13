package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.role.RoleResponseDto;
import java.util.List;

public interface RoleService {

    List<RoleResponseDto> findAll();
}