package com.tamachihua_erp.api.util;

import java.math.BigDecimal;

// Calcula el estado de la existencia según el contrato:
// OUT si stock = 0; LOW si stock <= minStock; OK en otro caso.
public final class StockStatusCalculator {

    private StockStatusCalculator() {
    }

    public static String calculate(BigDecimal stock, BigDecimal minStock) {

        BigDecimal safeStock = stock == null ? BigDecimal.ZERO : stock;

        if (safeStock.compareTo(BigDecimal.ZERO) == 0) {
            return "OUT";
        }

        if (minStock != null && safeStock.compareTo(minStock) <= 0) {
            return "LOW";
        }

        return "OK";
    }
}
