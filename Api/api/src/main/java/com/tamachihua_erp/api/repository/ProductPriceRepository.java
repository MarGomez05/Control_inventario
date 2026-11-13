package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.ProductPrice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductPriceRepository
        extends JpaRepository<ProductPrice, Long> {

    // Historial completo, más reciente primero; incluye el usuario que hizo el cambio
    @EntityGraph(attributePaths = "user")
    List<ProductPrice> findByProductPresentationIdOrderByStartDateDesc(Long productPresentationId);

    // Precio vigente (end_date IS NULL); el índice único parcial garantiza uno solo
    @EntityGraph(attributePaths = "user")
    Optional<ProductPrice> findByProductPresentationIdAndEndDateIsNull(Long productPresentationId);

    // Precios vigentes de varias presentaciones para armar el detalle del producto
    List<ProductPrice> findByProductPresentationIdInAndEndDateIsNull(
            Collection<Long> productPresentationIds
    );
}
