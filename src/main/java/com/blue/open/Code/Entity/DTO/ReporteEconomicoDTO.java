package com.blue.open.Code.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte economico: resumen, agrupaciones y la pagina de movimientos. Todos los importes salen de
 * movimientos reales de caja (lo efectivamente cobrado o pagado), nunca de cuotas pendientes.
 */
@Getter
@Setter
public class ReporteEconomicoDTO {

    private Resumen resumen = new Resumen();
    private List<Grupo> porCategoria = new ArrayList<>();
    private List<Grupo> porActividad = new ArrayList<>();
    private List<Grupo> porMedioPago = new ArrayList<>();
    /** Evolucion mensual, periodo yyyy-MM. */
    private List<Grupo> mensual = new ArrayList<>();
    private Page<ReporteMovimientoRowDTO> movimientos;

    @Getter
    @Setter
    public static class Resumen {
        private BigDecimal totalIngresos = BigDecimal.ZERO;
        private BigDecimal totalEgresos = BigDecimal.ZERO;
        /** totalIngresos - totalEgresos. */
        private BigDecimal balance = BigDecimal.ZERO;
        private long operaciones;
        private long cantidadIngresos;
        private long cantidadEgresos;
        private BigDecimal promedioIngreso = BigDecimal.ZERO;
        private BigDecimal promedioEgreso = BigDecimal.ZERO;
        private BigDecimal cuotasSociales = BigDecimal.ZERO;
        private BigDecimal cuotasActividades = BigDecimal.ZERO;
        private BigDecimal pases = BigDecimal.ZERO;
        private BigDecimal amenities = BigDecimal.ZERO;
        private BigDecimal eventos = BigDecimal.ZERO;
        private BigDecimal otrosIngresos = BigDecimal.ZERO;
    }

    /** Una agrupacion (categoria, actividad, medio de pago o mes) con sus totales. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Grupo {
        private String clave;
        private String nombre;
        private BigDecimal ingresos = BigDecimal.ZERO;
        private BigDecimal egresos = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private long operaciones;
    }
}
