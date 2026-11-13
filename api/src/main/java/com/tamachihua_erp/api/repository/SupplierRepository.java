package com.tamachihua_erp.api.repository;

import com.tamachihua_erp.api.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// Proveedores de mercadería (HU-09, contrato 6)
public interface SupplierRepository
        extends JpaRepository<Supplier, Long>,
        JpaSpecificationExecutor<Supplier> {

    List<Supplier> findAllByIsActiveTrueOrderByTradeNameAsc();

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Long id);

    boolean existsByNrc(String nrc);

    boolean existsByNrcAndIdNot(String nrc, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
