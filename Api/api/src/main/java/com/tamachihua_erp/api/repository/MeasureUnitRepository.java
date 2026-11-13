package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.MeasureUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MeasureUnitRepository
        extends JpaRepository<MeasureUnit, Long>,
        JpaSpecificationExecutor<MeasureUnit> {

    // Para los selectores (options): solo activas y ordenadas por nombre
    List<MeasureUnit> findAllByIsActiveTrueOrderByNameAsc();

    // Unicidad sin distinguir mayúsculas, igual que los índices lower(name) y lower(code)
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
