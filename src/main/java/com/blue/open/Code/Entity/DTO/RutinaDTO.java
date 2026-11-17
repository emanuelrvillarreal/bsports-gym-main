package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.AsignacionRutinaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DificultadEjercicioEnum;
import com.blue.open.Code.Entity.Enums.RutinaEstadoEnum;
import com.blue.open.Code.Entity.Enums.TipoEjercicioEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rutinas de entrenamiento: comandos (alta, edicion, asignacion, seguimiento) y respuestas. */
public final class RutinaDTO {

    private RutinaDTO() {
    }

    // ---------------------------------------------------------------- Comandos

    @Getter
    @Setter
    public static class ItemCommand {
        @NotBlank(message = "Falta el ejercicio")
        @Size(max = 36)
        private String ejercicioUuid;

        @Min(value = 1, message = "Las series van de 1 a 50")
        @Max(value = 50, message = "Las series van de 1 a 50")
        private Integer series;

        @Size(max = 40)
        private String repeticiones;

        @Size(max = 60)
        private String carga;

        @Min(value = 0, message = "El descanso no puede ser negativo")
        @Max(value = 3600, message = "El descanso puede ser de hasta 1 hora")
        private Integer descansoSegundos;

        @Size(max = 500)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class DiaCommand {
        @NotBlank(message = "Cada día necesita un nombre")
        @Size(max = 80)
        private String nombre;

        @NotEmpty(message = "Cada día necesita al menos un ejercicio")
        @Size(max = 40, message = "Hasta 40 ejercicios por día")
        @Valid
        private List<ItemCommand> items;
    }

    @Getter
    @Setter
    public static class Command {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        private String nombre;

        @Size(max = 4000)
        private String objetivo;

        @Size(max = 80)
        private String disciplina;

        @Size(max = 36)
        private String actividadUuid;

        /** Solo ADMIN: profesor responsable. Si crea un profesor, es el mismo. */
        @Size(max = 36)
        private String profesorUuid;

        private LocalDate fechaInicio;
        private LocalDate fechaFin;

        @Size(max = 4000)
        private String observaciones;

        @NotEmpty(message = "La rutina necesita al menos un día")
        @Size(max = 14, message = "Hasta 14 días por rutina")
        @Valid
        private List<DiaCommand> dias;

        /** Opcional al crear: socios a los que se asigna enseguida. */
        @Size(max = 200)
        private List<@NotBlank @Size(max = 36) String> memberUuids;
    }

    @Getter
    @Setter
    public static class AsignarCommand {
        @NotEmpty(message = "Elegí al menos un socio")
        @Size(max = 200)
        private List<@NotBlank @Size(max = 36) String> memberUuids;

        private LocalDate fechaInicio;
        private LocalDate fechaFin;

        @Size(max = 2000)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class SeguimientoCommand {
        @NotNull(message = "La fecha es obligatoria")
        private LocalDate fecha;

        @Size(max = 600)
        private List<@NotBlank @Size(max = 36) String> itemUuids;

        @Min(value = 1, message = "El esfuerzo va de 1 a 10")
        @Max(value = 10, message = "El esfuerzo va de 1 a 10")
        private Integer esfuerzo;

        @Size(max = 2000)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class PaginateRequest {
        @Size(max = 100)
        private String search;
        private RutinaEstadoEnum estado;
        @Size(max = 36)
        private String memberUuid;
        @Min(1)
        private int page = 1;
        @Min(1)
        @Max(100)
        private int size = 20;
    }

    // ---------------------------------------------------------------- Respuestas

    @Getter
    @Setter
    public static class Fila {
        private String uuid;
        private String nombre;
        private String disciplina;
        private String actividad;
        private String profesor;
        private RutinaEstadoEnum estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private int dias;
        private int ejercicios;
        private int asignacionesActivas;
        private Timestamp actualizada;
    }

    @Getter
    @Setter
    public static class EjercicioResumen {
        private String uuid;
        private String nombre;
        private TipoEjercicioEnum tipo;
        private DificultadEjercicioEnum dificultad;
        private String grupoMuscular;
        private String imagenUrl;
        private String videoUrl;
        private String indicaciones;
    }

    @Getter
    @Setter
    public static class Item {
        private String uuid;
        private int orden;
        private EjercicioResumen ejercicio;
        private Integer series;
        private String repeticiones;
        private String carga;
        private Integer descansoSegundos;
        private String observaciones;
    }

    @Getter
    @Setter
    public static class Dia {
        private String uuid;
        private int orden;
        private String nombre;
        private List<Item> items = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Seguimiento {
        private String uuid;
        private LocalDate fecha;
        private Integer esfuerzo;
        private String observaciones;
        private String registradoPor;
        private List<String> itemUuids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Asignacion {
        private String uuid;
        private String rutinaUuid;
        private String rutinaNombre;
        private String memberUuid;
        private String socio;
        private String dni;
        private AsignacionRutinaEstadoEnum estado;
        private LocalDate fechaAsignacion;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalDate fechaFinalizacion;
        private String observaciones;
        private String asignadaPor;
        private int sesiones;
        private LocalDate ultimaSesion;
        private List<Seguimiento> seguimientos = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Historial {
        private Timestamp fecha;
        private String accion;
        private String detalle;
        private String usuario;
    }

    @Getter
    @Setter
    public static class Detalle {
        private String uuid;
        private String nombre;
        private String objetivo;
        private String disciplina;
        private String actividadUuid;
        private String actividad;
        private String profesorUuid;
        private String profesor;
        private RutinaEstadoEnum estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String observaciones;
        private String reemplazaAUuid;
        private String reemplazaANombre;
        private boolean editable;
        private List<Dia> dias = new ArrayList<>();
        private List<Asignacion> asignaciones = new ArrayList<>();
        private List<Historial> historial = new ArrayList<>();
    }

    /** Socio al que el usuario puede asignar rutinas. */
    @Getter
    @Setter
    public static class Alumno {
        private String memberUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private String actividades;
    }

    /** Portal: una rutina asignada al socio con todo su contenido y su historial de sesiones. */
    @Getter
    @Setter
    public static class Portal {
        private Asignacion asignacion;
        private Detalle rutina;
    }
}
