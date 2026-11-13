package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.Presentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresentationRepository
        extends JpaRepository<Presentation, Long>,
        JpaSpecificationExecutor<Presentation> {

    // Trae la unidad de medida en la misma consulta (evita una consulta extra por fila)
    @Override
    @EntityGraph(attributePaths = "measureUnit")
    Page<Presentation> findAll(Specification<Presentation> specification, Pageable pageable);

    @EntityGraph(attributePaths = "measureUnit")
    List<Presentation> findAllByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Regla del contrato: no se desactiva una unidad que tenga presentaciones activas
    boolean existsByMeasureUnitIdAndIsActiveTrue(Long measureUnitId);

    // Regla del contrato: no se desactiva una presentación asignada a productos activos.
    // Consulta nativa porque la entidad ProductPresentation es de S2-05 (Marvin).
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM product_presentations
                WHERE presentation_id = :presentationId
                  AND is_active = TRUE
            )
            """, nativeQuery = true)
    boolean isUsedByActiveProducts(@Param("presentationId") Long presentationId);
}
