package com.tamachihua_erp.api.util;

// Calcula la severidad de vencimiento según el contrato 7 y 8:
// EXPIRED (< 0), RED (0 a 15), YELLOW (16 a 30) y NONE (sin fecha o más de 30 días).
public final class ExpirationSeverityCalculator {

    private ExpirationSeverityCalculator() {
    }

    public static String severity(Long daysToExpire) {

        if (daysToExpire == null || daysToExpire > 30) {
            return "NONE";
        }

        if (daysToExpire < 0) {
            return "EXPIRED";
        }

        if (daysToExpire <= 15) {
            return "RED";
        }

        return "YELLOW";
    }

    public static String label(String severity) {

        return switch (severity) {
            case "EXPIRED" -> "Vencido";
            case "RED", "YELLOW" -> "Vence pronto";
            default -> "Sin alerta";
        };
    }
}
