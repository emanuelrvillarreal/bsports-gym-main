package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Ficha del socio: estado de cuenta, actividades (con historial), membresías y últimas cuotas. */
@Getter
@Setter
public class MemberFichaDTO {

    private Cuenta cuenta = new Cuenta();
    private List<Actividad> actividades = new ArrayList<>();
    private List<MembresiaDTO.Membresia> membresias = new ArrayList<>();
    private List<CuotaItem> cuotas = new ArrayList<>();

    @Getter
    @Setter
    public static class Cuenta {
        /** Sin cuotas vencidas impagas. */
        private boolean alDia;
        private BigDecimal deudaVencida = BigDecimal.ZERO;
        private BigDecimal porVencer = BigDecimal.ZERO;
        private int cuotasVencidas;
        private int cuotasPorVencer;
        /** Próxima cuota impaga que vence (hoy o después). */
        private CuotaItem proximoVencimiento;
    }

    @Getter
    @Setter
    public static class Actividad {
        private String inscripcionUuid;
        private String actividad;
        private boolean activa;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaInscripcion;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaBaja;
        private String motivoBaja;
        /** Fecha en que se genera la próxima cuota (solo inscripciones activas). */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate proximaCuota;
        /** Hoy la cubre un pase que incluye la cuota de las actividades. */
        private boolean cubiertaPorPase;
        /** Última cuota no anulada de la actividad. */
        private CuotaItem ultimaCuota;
    }

    @Getter
    @Setter
    public static class CuotaItem {
        private String uuid;
        private String concepto;
        private CuotaTipoEnum tipo;
        private CuotaEstadoEnum estado;
        private BigDecimal monto;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate vencimiento;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaPago;
        private MedioPagoEnum medioPago;
        /** Impaga con vencimiento anterior a hoy. */
        private boolean vencida;
    }
}
