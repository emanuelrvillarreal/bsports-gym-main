package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Asistencia de un periodo de inscripcion (un alumno en una actividad). */
@Getter
@Setter
public class AsistenciaAlumnoFilaDTO {
    private String inscripcionUuid;
    private String memberUuid;
    private String nombre;
    private String apellido;
    private String dni;
    private String imageUrl;
    private String categoria;
    private String actividadUuid;
    private String actividadNombre;
    private List<String> profesores = new ArrayList<>();
    private LocalDate fechaInscripcion;
    private LocalDate fechaBaja;
    private String motivoBaja;
    private boolean activa;
    private long antiguedadDias;
    /** Ej. "2 años y 8 meses". */
    private String antiguedad;
    private int clases;
    private int presentes;
    /** Ausencias sin justificar. */
    private int ausentes;
    private int justificadas;
    /** presentes / clases x 100, con un decimal. null si todavia no hubo clases. */
    private Double porcentaje;
    /** VERDE, AMARILLO, ROJO o SIN_DATOS segun los umbrales del club. */
    private String nivel;
    private LocalDate ultimaAsistencia;
    private int ausenciasConsecutivas;
    /** Textos de las alertas activas, ej. "15 días sin asistir". */
    private List<String> alertas = new ArrayList<>();
}
