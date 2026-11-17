package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/** Resultado del seguimiento de asistencia: indicadores, rankings, evolucion y la pagina de alumnos. */
@Getter
@Setter
public class AsistenciaEstadisticasDTO {

    private Resumen resumen = new Resumen();
    private AsistenciaUmbralesDTO umbrales;
    private List<ActividadResumen> actividades = new ArrayList<>();
    private List<ProfesorResumen> profesores = new ArrayList<>();
    private List<AsistenciaMesDTO> evolucionMensual = new ArrayList<>();
    /** Alumnos con alertas (hasta 50), los mas urgentes primero. */
    private List<AsistenciaAlumnoFilaDTO> alertas = new ArrayList<>();
    private Page<AsistenciaAlumnoFilaDTO> alumnos;

    @Getter
    @Setter
    public static class Resumen {
        /** Socios distintos con inscripcion en el filtro. */
        private int sociosInscriptos;
        /** Socios distintos con al menos una inscripcion activa. */
        private int alumnosActivos;
        private int clasesRealizadas;
        private int presentes;
        private int ausentes;
        private int justificadas;
        /** Presentes sobre clases de todos los alumnos (promedio ponderado). null sin clases. */
        private Double promedioAsistencia;
        /** Inscripciones activas en rojo. */
        private int bajaAsistencia;
        /** Inscripciones activas con la alerta de dias sin asistir. */
        private int dejaronDeAsistir;
        private int conAlertas;
    }

    @Getter
    @Setter
    public static class ActividadResumen {
        private String actividadUuid;
        private String nombre;
        private List<String> profesores = new ArrayList<>();
        private int inscriptos;
        private int clases;
        private Double promedioAsistencia;
    }

    @Getter
    @Setter
    public static class ProfesorResumen {
        private String profesorUuid;
        private String nombre;
        private int actividades;
        private int alumnos;
        private Double promedioAsistencia;
    }
}
