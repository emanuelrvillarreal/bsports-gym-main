package com.blue.open.Code.Entity.Enums;

import java.time.LocalDate;

/** Duracion de un pase. Las modalidades por ingresos o personalizadas usan {@code duracionDias}. */
public enum PaseModalidadEnum {
    DIARIO(0, 1),
    SEMANAL(0, 7),
    MENSUAL(1, 0),
    TRIMESTRAL(3, 0),
    SEMESTRAL(6, 0),
    ANUAL(12, 0),
    POR_INGRESOS(0, 0),
    PERSONALIZADO(0, 0);

    private final int meses;
    private final int dias;

    PaseModalidadEnum(int meses, int dias) {
        this.meses = meses;
        this.dias = dias;
    }

    public boolean requiereDuracionDias() {
        return meses == 0 && dias == 0;
    }

    /** Ultimo dia de vigencia (inclusive) para un pase que arranca en {@code inicio}. */
    public LocalDate fin(LocalDate inicio, Integer duracionDias) {
        if (meses > 0) {
            return inicio.plusMonths(meses).minusDays(1);
        }
        int total = requiereDuracionDias() ? (duracionDias == null ? 0 : duracionDias) : dias;
        if (total <= 0) {
            throw new IllegalArgumentException("La modalidad " + name() + " necesita una duracion en dias");
        }
        return inicio.plusDays(total - 1L);
    }
}
