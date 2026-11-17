package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Filtros del seguimiento de asistencia. Todos son opcionales y se combinan.
 * Sin fechas el calculo va desde la inscripcion de cada alumno hasta hoy.
 */
@Getter
@Setter
public class AsistenciaEstadisticasRequestDTO {

    private LocalDate desde;
    private LocalDate hasta;

    @Size(max = 36)
    private String actividadUuid;

    /** Solo backoffice: el profesor siempre ve sus actividades. */
    @Size(max = 36)
    private String profesorUuid;

    /** Categoria del socio. */
    @Size(max = 36)
    private String categoriaUuid;

    @Size(max = 36)
    private String memberUuid;

    @Size(max = 100)
    private String search;

    /** ACTIVAS (por defecto) o TODAS (incluye periodos ya cerrados). */
    @Pattern(regexp = "ACTIVAS|TODAS", message = "Estado de inscripción inválido")
    private String estadoInscripcion = "ACTIVAS";

    /** VERDE, AMARILLO, ROJO o SIN_DATOS. */
    @Pattern(regexp = "VERDE|AMARILLO|ROJO|SIN_DATOS", message = "Nivel inválido")
    private String nivel;

    /** Solo alumnos con alguna alerta de inasistencia. */
    private boolean soloAlertas;

    /** NOMBRE (por defecto), ASISTENCIA_ASC o ASISTENCIA_DESC. */
    @Pattern(regexp = "NOMBRE|ASISTENCIA_ASC|ASISTENCIA_DESC", message = "Orden inválido")
    private String orden = "NOMBRE";

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;
}
