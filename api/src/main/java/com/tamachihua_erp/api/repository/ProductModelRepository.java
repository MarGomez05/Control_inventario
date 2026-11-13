package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductModelRepository
        extends JpaRepository<ProductModel, Long>,
        JpaSpecificationExecutor<ProductModel> {

    // Trae la marca en la misma consulta (evita una consulta extra por fila)
    @Override
    @EntityGraph(attributePaths = "productBrand")
    Page<ProductModel> findAll(Specification<ProductModel> specification, Pageable pageable);

    // Options: solo modelos activos de marcas activas
    @EntityGraph(attributePaths = "productBrand")
    List<ProductModel> findAllByIsActiveTrueAndProductBrandIsActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "productBrand")
    List<ProductModel> findAllByProductBrandIdAndIsActiveTrueAndProductBrandIsActiveTrueOrderByNameAsc(Long brandId);

    // Nombre único dentro de la misma marca (índice uq_product_models_brand_name)
    boolean existsByProductBrandIdAndNameIgnoreCase(Long brandId, String name);

    boolean existsByProductBrandIdAndNameIgnoreCaseAndIdNot(Long brandId, String name, Long id);

    // Regla del contrato: no se desactiva una marca con modelos activos
    boolean existsByProductBrandIdAndIsActiveTrue(Long brandId);

    // Regla del contrato: no se desactiva un modelo usado por productos activos.
    // Consulta nativa porque la entidad Product es de S2-05 (Marvin).
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM products
                WHERE product_model_id = :modelId
                  AND is_active = TRUE
            )
            """, nativeQuery = true)
    boolean isUsedByActiveProducts(@Param("modelId") Long modelId);
}
