package com.blue.open.Code.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Indicadores del panel de administracion. */
public final class DashboardDTO {

    private DashboardDTO() {
    }

    // ---------------------------------------------------------------- Resumen operativo (ADMIN y recepcion)

    /** Contrato que ya consumia el panel de recepcion (resumen-dashboard). */
    @Getter
    @Setter
    public static class Resumen {
        private Socios socios = new Socios();
        private Actividades actividades = new Actividades();
        /** Solo para ADMIN; null para recepcion. */
        private Financiero financiero;
        private Accesos accesos = new Accesos();
        private Reservas reservas = new Reservas();
    }

    @Getter @Setter
    public static class Socios {
        private long activos;
        private long morosos;
        private long inhabilitados;
    }

    @Getter @Setter
    public static class Actividades {
        private long activas;
        private long cuposDisponibles;
        private long inscripciones;
        private long proximosEventos;
    }

    @Getter @Setter
    public static class Financiero {
        private BigDecimal ingresos = BigDecimal.ZERO;
        private BigDecimal egresos = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private BigDecimal deudaVencida = BigDecimal.ZERO;
    }

    @Getter @Setter
    public static class Accesos {
        private long presentes;
    }

    @Getter @Setter
    public static class Reservas {
        private long proximas;
    }

    // ---------------------------------------------------------------- Dashboard general (ADMIN)

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String clave;
        private String nombre;
        private long valor;
        /** Segundo valor opcional (ej. cupo, bajas del mes). */
        private long extra;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mes {
        /** yyyy-MM. */
        private String periodo;
        private long altas;
        private long bajas;
        private long inscripciones;
        private BigDecimal ingresos = BigDecimal.ZERO;
        private BigDecimal egresos = BigDecimal.ZERO;
        private Double asistencia;
    }

    @Getter
    @Setter
    public static class General {
        // Socios
        private long sociosActivos;
        private long sociosNuevosMes;
        private long sociosBajasMes;
        private long sociosMorosos;
        private List<Item> sociosPorCategoria = new ArrayList<>();
        // Actividades
        private long actividades;
        private long inscriptos;
        private long profesoresActivos;
        /** Inscriptos por actividad (extra = cupo total de sus horarios), de mayor a menor. */
        private List<Item> demandaActividades = new ArrayList<>();
        // Asistencia (ultimos 3 meses)
        private Double asistenciaPromedio;
        private long bajaAsistencia;
        private long dejaronDeAsistir;
        // Deportivo
        private long equiposActivos;
        private long jugadoresEnPlanteles;
        private long torneosEnCurso;
        private List<Item> participacionPorDisciplina = new ArrayList<>();
        // Economia (mes actual y acumulado del año)
        private BigDecimal ingresosMes = BigDecimal.ZERO;
        private BigDecimal egresosMes = BigDecimal.ZERO;
        private BigDecimal balanceMes = BigDecimal.ZERO;
        private BigDecimal ingresosAnio = BigDecimal.ZERO;
        private BigDecimal egresosAnio = BigDecimal.ZERO;
        private BigDecimal deudaVencida = BigDecimal.ZERO;
        // Alertas operativas
        private long mantenimientosVencidos;
        private long evaluacionesVencidas;
        /** Ultimos 12 meses: socios, inscripciones, economia y asistencia. */
        private List<Mes> evolucion = new ArrayList<>();
    }
}
