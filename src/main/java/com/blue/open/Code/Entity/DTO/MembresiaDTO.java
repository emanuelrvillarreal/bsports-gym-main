package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaOrigenEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;
import com.blue.open.Code.Entity.Enums.PaseCategoriaEnum;
import com.blue.open.Code.Entity.Enums.PaseModalidadEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pases (oferta del club) y membresias (pases contratados): comandos y respuestas. */
public final class MembresiaDTO {

    private MembresiaDTO() {
    }

    // ------------------------------------------------------------------ Pases

    @Getter
    @Setter
    public static class PaseCommand {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        private String nombre;
        @Size(max = 2000)
        private String descripcion;
        @NotNull(message = "Elegí la categoría")
        private PaseCategoriaEnum categoria;
        @NotNull(message = "Elegí la duración")
        private PaseModalidadEnum modalidad;
        @Min(value = 1, message = "La duración debe ser de al menos 1 día")
        @Max(value = 3660, message = "La duración no puede superar 10 años")
        private Integer duracionDias;
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        private BigDecimal precio;
        @Min(value = 1, message = "La cantidad de ingresos debe ser al menos 1")
        @Max(value = 10000, message = "Cantidad de ingresos demasiado grande")
        private Integer cantidadIngresos;
        private Set<DiaSemanaEnum> diasPermitidos;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime horaDesde;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime horaHasta;
        private boolean renovacionAutomatica;
        private boolean congelamientoExtiende = true;
        private boolean incluyeCuotaActividades;
        private boolean familiar;
        @Min(value = 2, message = "Un pase familiar cubre al menos 2 personas")
        @Max(value = 20, message = "Máximo 20 integrantes")
        private Integer maxIntegrantes;
        private boolean ventaPortal;
        private LocalDate ventaDesde;
        private LocalDate ventaHasta;
        @Size(max = 2000)
        private String restricciones;
        private boolean activo = true;
        private List<String> actividadesUuids = new ArrayList<>();
        private List<String> amenitiesUuids = new ArrayList<>();
        private List<String> descuentosUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Referencia {
        private String uuid;
        private String nombre;

        public Referencia() {
        }

        public Referencia(String uuid, String nombre) {
            this.uuid = uuid;
            this.nombre = nombre;
        }
    }

    @Getter
    @Setter
    public static class Pase {
        private String uuid;
        private String nombre;
        private String descripcion;
        private PaseCategoriaEnum categoria;
        private PaseModalidadEnum modalidad;
        private Integer duracionDias;
        private BigDecimal precio;
        private Integer cantidadIngresos;
        private List<DiaSemanaEnum> diasPermitidos = new ArrayList<>();
        @JsonFormat(pattern = "HH:mm")
        private LocalTime horaDesde;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime horaHasta;
        private boolean renovacionAutomatica;
        private boolean congelamientoExtiende;
        private boolean incluyeCuotaActividades;
        private boolean familiar;
        private Integer maxIntegrantes;
        private boolean ventaPortal;
        private LocalDate ventaDesde;
        private LocalDate ventaHasta;
        private String restricciones;
        private boolean activo;
        /** Activo y dentro de las fechas de venta. */
        private boolean disponible;
        private List<Referencia> actividades = new ArrayList<>();
        private List<Referencia> amenities = new ArrayList<>();
        private List<Referencia> descuentos = new ArrayList<>();
        private long membresiasVigentes;
    }

    // ------------------------------------------------------------------ Membresias: comandos

    @Getter
    @Setter
    public static class ContratarCommand {
        @NotBlank(message = "Elegí el socio")
        private String memberUuid;
        @NotBlank(message = "Elegí el pase")
        private String paseUuid;
        /** Por defecto, hoy. */
        private LocalDate fechaInicio;
        /** Solo pases familiares: otros integrantes del grupo cubiertos. */
        private List<String> integrantesUuids = new ArrayList<>();
        private List<String> descuentosUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class RenovarCommand {
        private List<String> descuentosUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class CambiarPlanCommand {
        @NotBlank(message = "Elegí el nuevo pase")
        private String paseUuid;
        private LocalDate fechaInicio;
        private List<String> integrantesUuids = new ArrayList<>();
        private List<String> descuentosUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class CongelarCommand {
        @NotNull(message = "Indicá desde cuándo")
        private LocalDate desde;
        @NotNull(message = "Indicá hasta cuándo")
        private LocalDate hasta;
        @NotBlank(message = "Indicá el motivo")
        @Size(max = 255)
        private String motivo;
    }

    @Getter
    @Setter
    public static class BajaCommand {
        @NotBlank(message = "Indicá el motivo de la baja")
        @Size(max = 255)
        private String motivo;
    }

    @Getter
    @Setter
    public static class PortalContratarCommand {
        @NotBlank(message = "Elegí el pase")
        private String paseUuid;
        /** Socio del grupo para quien es el pase; por defecto, el titular. */
        private String memberUuid;
        private LocalDate fechaInicio;
        private List<String> integrantesUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Config {
        /** Si es true, un socio sin pase vigente no puede ingresar con el QR. */
        private boolean exigirPaseParaIngresar;
    }

    @Getter
    @Setter
    public static class PaginateRequest {
        private int page;
        @Min(1)
        @Max(200)
        private int size = 20;
        private String search;
        private String paseUuid;
        private MembresiaSituacionEnum situacion;
        private MembresiaOrigenEnum origen;
        /** Período por fecha de inicio de la membresía. */
        private LocalDate desde;
        private LocalDate hasta;
    }

    // ------------------------------------------------------------------ Membresias: respuestas

    @Getter
    @Setter
    public static class Cobro {
        private String cuotaUuid;
        private CuotaEstadoEnum estado;
        private BigDecimal monto;
        private BigDecimal montoFinal;
        private LocalDate vencimiento;
        private LocalDate fechaPago;
        private String medioPago;
    }

    @Getter
    @Setter
    public static class Congelamiento {
        private String uuid;
        private LocalDate desde;
        private LocalDate hasta;
        private int dias;
        private String motivo;
        private boolean extiendeVencimiento;
        private String autorizadoPor;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
        private LocalDateTime registrado;
    }

    @Getter
    @Setter
    public static class Acceso {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
        private LocalDateTime fecha;
        private boolean permitido;
        private String motivo;
        private String socio;
    }

    @Getter
    @Setter
    public static class Membresia {
        private String uuid;
        private Pase pase;
        private Referencia socio;
        private String numeroSocio;
        private List<Referencia> integrantes = new ArrayList<>();
        private MembresiaOrigenEnum origen;
        private MembresiaEstadoEnum estado;
        private MembresiaSituacionEnum situacion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalDate fechaFinOriginal;
        private Integer ingresosPermitidos;
        private int ingresosUsados;
        private Integer ingresosDisponibles;
        private boolean renovacionAutomatica;
        private BigDecimal precio;
        private LocalDate fechaBaja;
        private String motivoBaja;
        private String membresiaAnteriorUuid;
        /** Ya tiene una renovación o un plan nuevo encadenado. */
        private boolean renovada;
        private Cobro cobro;
        private List<Congelamiento> congelamientos = new ArrayList<>();
        private List<Acceso> ultimosAccesos = new ArrayList<>();
        /** Acciones habilitadas para quien consulta. */
        private boolean puedeRenovar;
        private boolean puedeCongelar;
        private boolean puedeCambiarPlan;
        private boolean puedeDarDeBaja;
    }

    @Getter
    @Setter
    public static class Legajo {
        private Referencia socio;
        private boolean titular;
        private List<Referencia> grupo = new ArrayList<>();
        private Membresia vigente;
        private List<Membresia> membresias = new ArrayList<>();
        private boolean exigirPaseParaIngresar;
    }

    @Getter
    @Setter
    public static class Fila {
        private String uuid;
        private String socio;
        private String socioUuid;
        private String numeroSocio;
        private String pase;
        private String paseUuid;
        private PaseCategoriaEnum categoria;
        private MembresiaOrigenEnum origen;
        private MembresiaSituacionEnum situacion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private BigDecimal precio;
        private BigDecimal cobrado;
        private CuotaEstadoEnum estadoCobro;
        private Integer ingresosPermitidos;
        private int ingresosUsados;
        private int integrantes;
    }

    @Getter
    @Setter
    public static class Item {
        private String clave;
        private String nombre;
        private long cantidad;
        private BigDecimal importe = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    public static class Dashboard {
        private long activas;
        private long pendientesDePago;
        private long proximasAVencer;
        private long vencidasUltimos30Dias;
        private long congeladas;
        private long nuevasMes;
        private long renovacionesMes;
        private long bajasMes;
        private BigDecimal ingresosMes = BigDecimal.ZERO;
        private BigDecimal ingresosAnio = BigDecimal.ZERO;
        /** Cobrado en el año por cada pase. */
        private List<Item> ingresosPorPase = new ArrayList<>();
        /** Membresías contratadas en los últimos 12 meses por pase. */
        private List<Item> masContratados = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Portal {
        private List<Membresia> membresias = new ArrayList<>();
        private List<Pase> pasesDisponibles = new ArrayList<>();
        private List<Referencia> grupo = new ArrayList<>();
        private boolean puedeContratar;
    }

    /** Resultado del control de un ingreso por QR contra los pases del socio. */
    public record EvaluacionAcceso(boolean permitido, String motivo, String advertencia) {
    }
}
