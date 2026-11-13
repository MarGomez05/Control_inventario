package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductLot;
import com.tamachihua_erp.api.repository.projection.ExpiringLotProjection;
import com.tamachihua_erp.api.repository.projection.PresentationStockProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductLotRepository
        extends JpaRepository<ProductLot, Long>,
        JpaSpecificationExecutor<ProductLot> {

    // Existencia por presentación: suma de lotes activos con cantidad disponible > 0 (RN-09)
    @Query("""
            select l.productPresentation.id as productPresentationId,
                   sum(l.availableQuantity) as stock
            from ProductLot l
            where l.productPresentation.id in :productPresentationIds
              and l.isActive = true
              and l.availableQuantity > 0
            group by l.productPresentation.id
            """)
    List<PresentationStockProjection> sumStockByProductPresentationIds(
            @Param("productPresentationIds") Collection<Long> productPresentationIds
    );

    // Regla D-01: no se cambia requiresExpiration a FALSE si el producto tiene lotes activos
    @Query("""
            select count(l)
            from ProductLot l
            where l.productPresentation.product.id = :productId
              and l.isActive = true
              and l.availableQuantity > 0
            """)
    long countActiveLotsByProductId(@Param("productId") Long productId);

    // Listado de lotes con todas las asociaciones que necesita la respuesta
    @Override
    @EntityGraph(attributePaths = {
            "productPresentation",
            "productPresentation.product",
            "productPresentation.presentation",
            "supplier"
    })
    Page<ProductLot> findAll(Specification<ProductLot> specification, Pageable pageable);

    @EntityGraph(attributePaths = {
            "productPresentation",
            "productPresentation.product",
            "productPresentation.presentation",
            "supplier"
    })
    @Query("select l from ProductLot l where l.id = :id")
    Optional<ProductLot> findDetailedById(@Param("id") Long id);

    // Unicidad del lote por proveedor + número + presentación (P-07)
    boolean existsBySupplierIdAndLotNumberIgnoreCaseAndProductPresentationId(
            Long supplierId,
            String lotNumber,
            Long productPresentationId
    );

    // Último costo: todos los lotes del producto, del más reciente al más antiguo
    @EntityGraph(attributePaths = {
            "productPresentation",
            "productPresentation.presentation"
    })
    List<ProductLot> findByProductPresentationProductIdOrderByEntryDateDescIdDesc(Long productId);

    // Dashboard (HU-12): lotes activos = is_active y available_quantity > 0
    @Query(value = """
            SELECT COUNT(*)
            FROM product_lots l
            WHERE l.is_active = TRUE
              AND l.available_quantity > 0
            """, nativeQuery = true)
    long countActiveLots();

    @Query(value = """
            SELECT COUNT(*)
            FROM product_lots l
            WHERE l.is_active = TRUE
              AND l.available_quantity > 0
              AND l.expiration_date IS NOT NULL
              AND l.expiration_date >= :today
              AND l.expiration_date <= :limit
            """, nativeQuery = true)
    long countExpiringLots(
            @Param("today") LocalDate today,
            @Param("limit") LocalDate limit
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM product_lots l
            WHERE l.is_active = TRUE
              AND l.available_quantity > 0
              AND l.expiration_date IS NOT NULL
              AND l.expiration_date < :today
            """, nativeQuery = true)
    long countExpiredLots(@Param("today") LocalDate today);

    // Lotes vencidos y por vencer en la ventana de 30 días, vencidos primero
    @Query(value = """
            SELECT l.id AS "lotId",
                   p.name AS "productName",
                   pr.name AS "presentationName",
                   l.lot_number AS "lotNumber",
                   l.expiration_date AS "expirationDate"
            FROM product_lots l
            JOIN product_presentations pp ON pp.id = l.product_presentation_id
            JOIN products p ON p.id = pp.product_id
            JOIN presentations pr ON pr.id = pp.presentation_id
            WHERE l.is_active = TRUE
              AND l.available_quantity > 0
              AND l.expiration_date IS NOT NULL
              AND l.expiration_date <= :limit
            ORDER BY l.expiration_date ASC
            """, nativeQuery = true)
    List<ExpiringLotProjection> findExpiringLots(@Param("limit") LocalDate limit);
}
