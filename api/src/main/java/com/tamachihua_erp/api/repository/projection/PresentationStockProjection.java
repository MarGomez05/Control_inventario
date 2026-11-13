package com.tamachihua_erp.api.repository.projection;

import java.math.BigDecimal;

// Proyección de la consulta que suma existencias por presentación del producto
public interface PresentationStockProjection {

    Long getProductPresentationId();

    BigDecimal getStock();
}
