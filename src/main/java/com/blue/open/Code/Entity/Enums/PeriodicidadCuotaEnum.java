package com.blue.open.Code.Entity.Enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Cada cuánto se genera una cuota (social o de actividad). Se avanza por calendario, no por días
 * fijos, así las cuotas mantienen el día de la primera generación.
 */
public enum PeriodicidadCuotaEnum {
    SEMANAL(ChronoUnit.WEEKS, 1),
    MENSUAL(ChronoUnit.MONTHS, 1),
    BIMESTRAL(ChronoUnit.MONTHS, 2),
    TRIMESTRAL(ChronoUnit.MONTHS, 3),
    CUATRIMESTRAL(ChronoUnit.MONTHS, 4),
    SEMESTRAL(ChronoUnit.MONTHS, 6),
    ANUAL(ChronoUnit.MONTHS, 12);

    private final ChronoUnit unidad;
    private final int cantidad;

    PeriodicidadCuotaEnum(ChronoUnit unidad, int cantidad) {
        this.unidad = unidad;
        this.cantidad = cantidad;
    }

    /**
     * Fecha de la generación siguiente a {@code fecha}. Si el mes destino no tiene ese día
     * (29, 30 o 31), devuelve el último día del mes.
     */
    public LocalDate siguienteFecha(LocalDate fecha) {
        return fecha.plus(cantidad, unidad);
    }
}
