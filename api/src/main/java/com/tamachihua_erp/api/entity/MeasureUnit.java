package com.tamachihua_erp.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Unidad de medida (libra, kilogramo, galón...). Tabla measure_units del script 01_create_tables.sql
@Entity
@Table(name = "measure_units")
@Getter
@Setter
@NoArgsConstructor
public class MeasureUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Único sin distinguir mayúsculas (índice uq_measure_units_name)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // Abreviatura: LB, KG, GAL... Única sin distinguir mayúsculas (uq_measure_units_code)
    @Column(name = "code", nullable = false, length = 10)
    private String code;

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
