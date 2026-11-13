package com.tamachihua_erp.api.controller;

import com.tamachihua_erp.api.dto.role.RoleResponseDto;
import com.tamachihua_erp.api.service.RoleService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponseDto>> findAll() {

        return ResponseEntity.ok(roleService.findAll());
    }
}