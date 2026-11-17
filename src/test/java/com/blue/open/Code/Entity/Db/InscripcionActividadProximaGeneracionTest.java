package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InscripcionActividadProximaGeneracionTest {

    private Actividad actividad;
    private InscripcionActividad inscripcion;

    @BeforeEach
    void setUp() {
        actividad = new Actividad();
        actividad.setPeriodicidadCuota(PeriodicidadCuotaEnum.MENSUAL);

        inscripcion = new InscripcionActividad();
        inscripcion.setActividad(actividad);
        inscripcion.setProximaGeneracion(LocalDate.of(2026, 3, 17));
    }

    @Test
    void avanzarProximaGeneracion_conDiaFijoMensual_seAlineaAlUnoDelMesSiguiente() {
        actividad.setDiaVencimientoFijo(10);

        inscripcion.avanzarProximaGeneracion();

        assertEquals(LocalDate.of(2026, 4, 1), inscripcion.getProximaGeneracion());
    }

    @Test
    void avanzarProximaGeneracion_conDiaFijoYaAlineado_avanzaUnMes() {
        actividad.setDiaVencimientoFijo(10);
        inscripcion.setProximaGeneracion(LocalDate.of(2026, 4, 1));

        inscripcion.avanzarProximaGeneracion();

        assertEquals(LocalDate.of(2026, 5, 1), inscripcion.getProximaGeneracion());
    }

    @Test
    void avanzarProximaGeneracion_sinDiaFijo_conservaElDiaDeInscripcion() {
        inscripcion.avanzarProximaGeneracion();

        assertEquals(LocalDate.of(2026, 4, 17), inscripcion.getProximaGeneracion());
    }
}
