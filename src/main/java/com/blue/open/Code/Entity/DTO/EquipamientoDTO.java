package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MantenimientoTipoEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Equipamiento e infraestructura: comandos y respuestas. */
public final class EquipamientoDTO {

    private EquipamientoDTO() {
    }

    @Getter
    @Setter
    public static class Command {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        private String nombre;
        @Size(max = 40)
        private String codigo;
        @Size(max = 80)
        private String tipo;
        @Size(max = 80)
        private String disciplina;
        @Size(max = 80)
        private String marca;
        @Size(max = 80)
        private String modelo;
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        @Max(value = 100000, message = "Cantidad demasiado grande")
        private Integer cantidad;
        @NotNull(message = "Elegí el estado")
        private EquipamientoEstadoEnum estado;
        @Size(max = 120)
        private String ubicacion;
        private LocalDate fechaAdquisicion;
        @Size(max = 4000)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class MantenimientoCommand {
        @NotNull(message = "Elegí el tipo de mantenimiento")
        private MantenimientoTipoEnum tipo;
        @NotNull(message = "La fecha es obligatoria")
        private LocalDate fecha;
        @Size(max = 120)
        private String responsable;
        @NotBlank(message = "Describí qué se hizo")
        @Size(max = 4000)
        private String descripcion;
        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        private BigDecimal costo;
        private LocalDate proximoMantenimiento;
        /** Estado en que queda el equipo despues del mantenimiento (opcional). */
        private EquipamientoEstadoEnum estadoResultante;
        /** Registrar el costo como egreso en tesoreria (requiere permiso de egresos; los egresos no llevan medio de pago). */
        private boolean registrarEgreso;
    }

    @Getter
    @Setter
    public static class PaginateRequest {
        @Size(max = 100)
        private String search;
        private EquipamientoEstadoEnum estado;
        @Size(max = 80)
        private String disciplina;
        /** VENCIDO (proximo mantenimiento ya paso) o PROXIMO (en los proximos 30 dias). */
        @Pattern(regexp = "VENCIDO|PROXIMO", message = "Alerta inválida")
        private String alerta;
        private boolean incluirBajas;
        @Min(1)
        private int page = 1;
        @Min(1)
        @Max(100)
        private int size = 20;
    }

    @Getter
    @Setter
    public static class Fila {
        private String uuid;
        private String nombre;
        private String codigo;
        private String tipo;
        private String disciplina;
        private String marcaModelo;
        private int cantidad;
        private EquipamientoEstadoEnum estado;
        private String ubicacion;
        private LocalDate ultimoMantenimiento;
        private LocalDate proximoMantenimiento;
        /** VENCIDO, PROXIMO o null. */
        private String alerta;
        private BigDecimal costoMantenimientos = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    public static class Mantenimiento {
        private String uuid;
        private MantenimientoTipoEnum tipo;
        private LocalDate fecha;
        private String responsable;
        private String descripcion;
        private BigDecimal costo;
        private LocalDate proximoMantenimiento;
        private String registradoPor;
        private String movimientoUuid;
    }

    @Getter
    @Setter
    public static class Detalle {
        private String uuid;
        private String nombre;
        private String codigo;
        private String tipo;
        private String disciplina;
        private String marca;
        private String modelo;
        private int cantidad;
        private EquipamientoEstadoEnum estado;
        private String ubicacion;
        private LocalDate fechaAdquisicion;
        private String observaciones;
        private BigDecimal costoMantenimientos = BigDecimal.ZERO;
        private LocalDate proximoMantenimiento;
        private List<Mantenimiento> mantenimientos = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Resumen {
        private long items;
        private long unidades;
        private long enReparacion;
        private long fueraDeServicio;
        private long mantenimientosVencidos;
        private long mantenimientosProximos;
        private BigDecimal costoAnio = BigDecimal.ZERO;
    }
}
