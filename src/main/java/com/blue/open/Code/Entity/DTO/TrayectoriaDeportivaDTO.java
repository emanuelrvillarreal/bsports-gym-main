package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Trayectoria deportiva del socio dentro del club, armada con su historial de inscripciones. */
@Getter
@Setter
public class TrayectoriaDeportivaDTO {

    private String memberUuid;
    private String nombre;
    private String apellido;
    private String categoriaActual;
    private LocalDate fechaAltaClub;
    private String antiguedadClub;
    private int actividadesActuales;
    private int actividadesAnteriores;
    private List<ActividadTrayectoria> actividades = new ArrayList<>();
    /** Inscripciones, bajas y reincorporaciones en orden cronologico (lo mas nuevo primero). */
    private List<Hito> hitos = new ArrayList<>();

    @Getter
    @Setter
    public static class ActividadTrayectoria {
        private String actividadUuid;
        private String nombre;
        private boolean activa;
        private LocalDate primeraInscripcion;
        private LocalDate ultimaBaja;
        /** Desde la primera inscripcion hasta hoy (o hasta la ultima baja). */
        private String antiguedad;
        private long diasActivos;
        /** Suma de los periodos en que estuvo inscripto. */
        private String tiempoActivo;
        private int reinscripciones;
        private int interrupciones;
        private List<String> profesores = new ArrayList<>();
        private List<String> categorias = new ArrayList<>();
        private int clases;
        private int presentes;
        private int ausentes;
        private int justificadas;
        private Double porcentaje;
        private String nivel;
        private List<Periodo> periodos = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Periodo {
        private String inscripcionUuid;
        private LocalDate desde;
        private LocalDate hasta;
        private boolean activa;
        private String motivoBaja;
        private String duracion;
    }

    @Getter
    @Setter
    public static class Hito {
        private LocalDate fecha;
        /** INSCRIPCION, REINCORPORACION o BAJA. */
        private String tipo;
        private String actividad;
        private String detalle;
    }
}
