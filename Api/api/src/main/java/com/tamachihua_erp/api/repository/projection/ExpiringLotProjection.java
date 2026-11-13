package com.tamachihua_erp.api.repository.projection;

import java.time.LocalDate;

// Fila de la consulta de lotes vencidos y por vencer (dashboard)
public interface ExpiringLotProjection {

    Long getLotId();

    String getProductName();

    String getPresentationName();

    String getLotNumber();

    LocalDate getExpirationDate();
}
