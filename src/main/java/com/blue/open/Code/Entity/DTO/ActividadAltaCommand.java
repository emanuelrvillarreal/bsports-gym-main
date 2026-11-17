package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.Enums.RecargoTipoEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ActividadAltaCommand {

    @NotBlank(message = "Nombre requerido")
    @Size(max = 255, message = "Nombre demasiado largo")
    private String nombre;

    @NotBlank(message = "Ubicación requerida")
    @Size(max = 255, message = "Ubicación demasiado larga")
    private String ubicacion;

    /** Franjas en las que se dicta la actividad; cada una con su dia, horario y cupo. */
    @NotEmpty(message = "La actividad debe tener al menos un horario")
    @Size(max = 50, message = "Demasiados horarios para una actividad")
    @Valid
    private List<ActividadHorarioCommand> horarios;

    @NotNull(message = "Importe de cuota requerido")
    @DecimalMin(value = "0.01", message = "El importe de cuota debe ser mayor a cero")
    private BigDecimal importeCuota;

    /** Cada cuánto se genera la cuota. Opcional: si no viene, la actividad queda MENSUAL. */
    private PeriodicidadCuotaEnum periodicidadCuota;

    /** Días desde la generación hasta el vencimiento de la cuota. Opcional: si no viene, queda con 30. */
    @Min(value = 1, message = "Los días de vencimiento de cuota deben ser al menos 1")
    private Integer diasVencimientoCuota;

    /**
     * Dia del mes (1 a 28) en que vencen las cuotas. Opcional: si no viene, cada cuota vence
     * diasVencimientoCuota dias despues de generada. No se admite con periodicidad SEMANAL.
     */
    @Min(value = 1, message = "El día de vencimiento debe estar entre 1 y 28")
    @Max(value = 28, message = "El día de vencimiento debe estar entre 1 y 28")
    private Integer diaVencimientoFijo;

    /** Solo con dia fijo de vencimiento: si la inscripcion es despues de este dia, la primera cuota es la del mes siguiente. */
    @Min(value = 1, message = "El día de corte debe estar entre 1 y 28")
    @Max(value = 28, message = "El día de corte debe estar entre 1 y 28")
    private Integer diaCorteInscripcion;

    private List<@NotBlank(message = "UUID de profesor inválido") String> profesorUuids;

    /** Categorías de la actividad. Opcional: si no viene o viene vacía, la actividad queda sin categorías. */
    private List<@NotBlank(message = "UUID de categoría inválido") @Size(max = 36, message = "UUID de categoría inválido") String> categoriaUuids;

    /** Recargo por pago fuera de término de las cuotas de esta actividad. Opcional: null conserva lo actual. */
    private Boolean recargoHabilitado;

    private RecargoTipoEnum recargoTipo;

    @DecimalMin(value = "0.01", message = "El valor del recargo debe ser mayor a cero")
    private BigDecimal recargoValor;

    @Min(value = 0, message = "Los días de gracia del recargo no pueden ser negativos")
    private Integer recargoDiasGracia;

    /** Si la actividad exige certificado médico vigente. Opcional: null conserva lo actual (en el alta queda en false). */
    private Boolean requiereCertificado;

}
