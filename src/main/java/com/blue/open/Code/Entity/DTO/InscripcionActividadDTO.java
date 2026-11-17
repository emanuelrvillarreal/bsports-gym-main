package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class InscripcionActividadDTO {

    private String inscripcionUuid;
    private String actividadUuid;
    private String actividadNombre;
    private String memberUuid;
    private String memberNombre;
    private String memberApellido;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaInscripcion;
    private boolean actividadCompleta;
    private boolean cuotaGenerada;
    private String cuotaUuid;
    private String cuotaAdvertencia;
    /** Horarios que eligio el socio. */
    private List<String> horarioUuids;
    /** Todos los horarios de la actividad con su cupo actual, para mostrar o cambiar la eleccion. */
    private List<ActividadHorarioDTO> horarios;
}
