package com.tamachihua_erp.api.service.impl;

import com.tamachihua_erp.api.dto.supplier.SupplierOptionDto;
import com.tamachihua_erp.api.dto.supplier.SupplierRequestDto;
import com.tamachihua_erp.api.dto.supplier.SupplierResponseDto;
import com.tamachihua_erp.api.entity.Supplier;
import com.tamachihua_erp.api.exception.ResourceConflictException;
import com.tamachihua_erp.api.exception.ResourceNotFoundException;
import com.tamachihua_erp.api.mapper.SupplierMapper;
import com.tamachihua_erp.api.repository.SupplierRepository;
import com.tamachihua_erp.api.service.SupplierService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    @Override
    public Page<SupplierResponseDto> findAll(
            String search,
            Boolean isActive,
            Pageable pageable
    ) {

        Specification<Supplier> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {

            String pattern = "%" + search.trim().toLowerCase() + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("tradeName")), pattern),
                            cb.like(cb.lower(root.get("legalName")), pattern),
                            cb.like(cb.lower(root.get("nit")), pattern)
                    )
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
    public List<SupplierOptionDto> findActiveOptions() {

        return repository.findAllByIsActiveTrueOrderByTradeNameAsc()
                .stream()
                .map(mapper::toOptionDto)
                .toList();
    }

    @Override
    public SupplierResponseDto findById(Long id) {

        return mapper.toResponseDto(findSupplierOrThrow(id));
    }

    @Override
    @Transactional
    public SupplierResponseDto create(SupplierRequestDto dto) {

        validateUniqueFields(dto, null);

        Supplier supplier = new Supplier();

        applyChanges(supplier, dto);

        return mapper.toResponseDto(repository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponseDto update(Long id, SupplierRequestDto dto) {

        Supplier existing = findSupplierOrThrow(id);

        validateUniqueFields(dto, id);

        applyChanges(existing, dto);

        return mapper.toResponseDto(repository.save(existing));
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Boolean isActive) {

        Supplier supplier = findSupplierOrThrow(id);

        supplier.setIsActive(isActive);

        repository.save(supplier);
    }

    private void applyChanges(Supplier supplier, SupplierRequestDto dto) {

        supplier.setTradeName(dto.tradeName().trim());
        supplier.setLegalName(dto.legalName().trim());
        supplier.setNit(dto.nit().trim());
        supplier.setNrc(normalize(dto.nrc()));
        supplier.setAddress(normalize(dto.address()));
        supplier.setPhone(normalize(dto.phone()));
        supplier.setEmail(normalize(dto.email()));
        supplier.setCreditDays(dto.creditDays());
    }

    private void validateUniqueFields(SupplierRequestDto dto, Long id) {

        String nit = dto.nit().trim();

        boolean nitTaken = id == null
                ? repository.existsByNit(nit)
                : repository.existsByNitAndIdNot(nit, id);

        if (nitTaken) {
            throw new ResourceConflictException("Ya existe un proveedor con ese NIT");
        }

        String nrc = normalize(dto.nrc());

        if (nrc != null) {

            boolean nrcTaken = id == null
                    ? repository.existsByNrc(nrc)
                    : repository.existsByNrcAndIdNot(nrc, id);

            if (nrcTaken) {
                throw new ResourceConflictException("Ya existe un proveedor con ese NRC");
            }
        }

        String email = normalize(dto.email());

        if (email != null) {

            boolean emailTaken = id == null
                    ? repository.existsByEmailIgnoreCase(email)
                    : repository.existsByEmailIgnoreCaseAndIdNot(email, id);

            if (emailTaken) {
                throw new ResourceConflictException("Ya existe un proveedor con ese correo");
            }
        }
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Supplier findSupplierOrThrow(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el proveedor con ID: " + id
                ));
    }
}
