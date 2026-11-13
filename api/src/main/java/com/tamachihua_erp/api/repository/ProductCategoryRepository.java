package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductCategoryRepository
        // Extiende de esta clase que identifica la entidad y el tipo de dato de su primary key
        extends JpaRepository<ProductCategory, Long>,
        // Lo agregamos pensando en los filtros que necesitaremos posteriormente.
        JpaSpecificationExecutor<ProductCategory> {

    // Spring Data JPA interpreta el nombre del metodo y construye la consulta automáticamente.
    List<ProductCategory> findAllByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

