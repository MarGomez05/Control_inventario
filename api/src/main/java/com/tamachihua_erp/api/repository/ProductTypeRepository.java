package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// Tipos de producto: CRUD completo del contrato 4.2
public interface ProductTypeRepository
        extends JpaRepository<ProductType, Long>,
        JpaSpecificationExecutor<ProductType> {

    List<ProductType> findAllByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
