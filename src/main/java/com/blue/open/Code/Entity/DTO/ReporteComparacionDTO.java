package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Comparacion de dos periodos con los mismos filtros (ej. marzo 2025 vs marzo 2026). */
public final class ReporteComparacionDTO {

    private ReporteComparacionDTO() {
    }

    @Getter
    @Setter
    public static class Request {
        @NotNull(message = "Período A requerido")
        @Valid
        private Periodo periodoA;

        @NotNull(message = "Período B requerido")
        @Valid
        private Periodo periodoB;

        /** Resto de filtros; sus fechas se ignoran. */
        @Valid
        private ReporteEconomicoRequestDTO filtros;
    }

    @Getter
    @Setter
    public static class Periodo {
        @NotNull(message = "Fecha desde requerida")
        private LocalDate desde;
        @NotNull(message = "Fecha hasta requerida")
        private LocalDate hasta;
    }

    @Getter
    @Setter
    public static class Totales {
        private LocalDate desde;
        private LocalDate hasta;
        private BigDecimal ingresos = BigDecimal.ZERO;
        private BigDecimal egresos = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private long operaciones;
    }

    @Getter
    @Setter
    public static class Respuesta {
        private Totales periodoA;
        private Totales periodoB;
        /** B - A. */
        private BigDecimal diferenciaIngresos;
        private BigDecimal diferenciaEgresos;
        private BigDecimal diferenciaBalance;
        /** (B - A) / A x 100, con un decimal. Null si A es cero. */
        private Double variacionIngresos;
        private Double variacionEgresos;
        private Double variacionBalance;
    }
}
