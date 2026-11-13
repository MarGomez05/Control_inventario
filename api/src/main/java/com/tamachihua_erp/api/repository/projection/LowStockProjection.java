package com.tamachihua_erp.api.repository.projection;

import java.math.BigDecimal;

// Fila de la consulta de bajo stock (dashboard y alertas de la app)
public interface LowStockProjection {

    Long getProductId();

    String getProductCode();

    String getProductName();

    String getPresentationName();

    BigDecimal getCurrentStock();

    BigDecimal getMinStock();
}
