package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductBrandRepository
        extends JpaRepository<ProductBrand, Long>,
        JpaSpecificationExecutor<ProductBrand> {

    List<ProductBrand> findAllByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
