
package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.category.ProductCategoryRequestDto;
import com.tamachihua_erp.api.dto.category.ProductCategoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

// Esta interfaz define qué operaciones ofrece nuestro servicio
public interface ProductCategoryService {

    // Consultar categorías con filtros y paginación.
    Page<ProductCategoryResponseDto> findAll(
            String search,
            Boolean isActive,
            Pageable pageable
    );

    // Consultar categorías activas para selectores.
    List<ProductCategoryResponseDto> findActiveOptions();

    // Buscar una categoría específica.
    ProductCategoryResponseDto findById(Long id);

    // Registrar una categoría.
    ProductCategoryResponseDto create(ProductCategoryRequestDto dto);

    // Actualizar una categoría existente.
    ProductCategoryResponseDto update(
            Long id,
            ProductCategoryRequestDto dto
    );

    // Activar o desactivar una categoría, no la elimina
    void changeStatus(Long id, Boolean isActive);
}

