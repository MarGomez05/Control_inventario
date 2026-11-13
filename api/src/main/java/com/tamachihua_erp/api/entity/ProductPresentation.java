package com.tamachihua_erp.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Presentación en que se vende un producto, con su stock mínimo. Tabla product_presentations (HU-07).
// La existencia no se guarda: es la suma de available_quantity de sus lotes activos (RN-09).
@Entity
@Table(name = "product_presentations")
@Getter
@Setter
@NoArgsConstructor
public class ProductPresentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private Presentation presentation;

    // Código único en todo el sistema (uq_product_presentations_code). Se usa en la búsqueda de la app.
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    // Stock mínimo de esta presentación; CHECK min_stock > 0
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal minStock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
