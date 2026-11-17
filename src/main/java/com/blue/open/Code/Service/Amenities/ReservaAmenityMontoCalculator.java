package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.DTO.ReservaAmenityMontoResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class ReservaAmenityMontoCalculator {

    private static final int SCALE = 2;

    private ReservaAmenityMontoCalculator() {
    }

    public static ReservaAmenityMontoResult calcular(Amenity amenity, LocalDateTime inicio, LocalDateTime fin) {
        if (amenity == null || inicio == null || fin == null) {
            throw new IllegalArgumentException("Parámetros requeridos para cálculo de monto");
        }
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Fin debe ser posterior a inicio");
        }

        if (amenity.getMontoHora().compareTo(BigDecimal.ZERO) == 0) {
            ReservaAmenityMontoResult result = new ReservaAmenityMontoResult();
            result.setMontoTotal(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
            result.setMontoSena(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
            result.setSaldoPendiente(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
            return result;
        }

        long minutos = Duration.between(inicio, fin).toMinutes();
        long minutosFacturables = Math.max(60, minutos);
        BigDecimal horasFacturables = BigDecimal.valueOf(minutosFacturables)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal montoTotal = amenity.getMontoHora()
                .multiply(horasFacturables)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montoSena = montoTotal
                .multiply(amenity.getPorcentajeSena())
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);

        ReservaAmenityMontoResult result = new ReservaAmenityMontoResult();
        result.setMontoTotal(montoTotal);
        result.setMontoSena(montoSena);
        result.setSaldoPendiente(montoTotal);
        return result;
    }

    public static BigDecimal saldoTrasSena(BigDecimal montoTotal, BigDecimal montoSena) {
        return montoTotal.subtract(montoSena).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
