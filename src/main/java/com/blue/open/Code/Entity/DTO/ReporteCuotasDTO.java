package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de cuotas. Separa lo generado (cuotas emitidas en el periodo) de lo cobrado
 * (movimientos de caja activos asociados a cuotas, por fecha de cobro). Una cuota pendiente o
 * vencida nunca suma como cobrada.
 */
public final class ReporteCuotasDTO {

    private ReporteCuotasDTO() {
    }

    @Getter
    @Setter
    public static class Request {
        private LocalDate desde;
        private LocalDate hasta;
        private String actividadUuid;
        /** SOCIAL o ACTIVIDAD. Null = ambas. */
        private String tipo;
    }

    @Getter
    @Setter
    public static class Respuesta {
        private Estado generadas = new Estado();
        private Estado pagadas = new Estado();
        private Estado pendientes = new Estado();
        private Estado vencidas = new Estado();
        private Estado anuladas = new Estado();
        /** Suma de descuentos aplicados a las cuotas generadas (no anuladas). */
        private BigDecimal descuentosAplicados = BigDecimal.ZERO;
        /** Dinero efectivamente cobrado por cuotas en el periodo. */
        private BigDecimal recaudado = BigDecimal.ZERO;
        private long cobros;
        private List<ReporteEconomicoDTO.Grupo> recaudacionMensual = new ArrayList<>();
        private List<ReporteEconomicoDTO.Grupo> recaudacionPorActividad = new ArrayList<>();
        private List<ReporteEconomicoDTO.Grupo> recaudacionPorCategoriaSocio = new ArrayList<>();
        private List<ReporteEconomicoDTO.Grupo> recaudacionPorMedioPago = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Estado {
        private long cantidad;
        /** Importe a cobrar (monto de lista menos descuentos). */
        private BigDecimal importe = BigDecimal.ZERO;
    }
}
