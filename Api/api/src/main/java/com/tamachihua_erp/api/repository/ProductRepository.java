package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // Trae categoría y tipo en la misma consulta para el listado
    @Override
    @EntityGraph(attributePaths = {"productCategory", "productType"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    // Detalle con categoría, tipo, modelo y marca del modelo
    @EntityGraph(attributePaths = {
            "productCategory",
            "productType",
            "productModel",
            "productModel.productBrand"
    })
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    // Código único; se compara sin distinguir mayúsculas para evitar duplicados confusos
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // Regla del contrato: no se desactiva una categoría con productos activos
    boolean existsByProductCategoryIdAndIsActiveTrue(Long categoryId);

    // Regla del contrato: no se desactiva un tipo de producto con productos activos
    boolean existsByProductTypeIdAndIsActiveTrue(Long productTypeId);

    // Dashboard (HU-12): productos activos
    long countByIsActiveTrue();
}
