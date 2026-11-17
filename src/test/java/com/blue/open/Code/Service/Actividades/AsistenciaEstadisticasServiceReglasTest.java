package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.AsistenciaUmbralesDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Reglas puras del seguimiento: porcentaje, semaforo y texto de antiguedad. */
class AsistenciaEstadisticasServiceReglasTest {

    @Test
    void porcentajeEsPresentesSobreClasesConUnDecimal() {
        assertEquals(85.0, AsistenciaEstadisticasService.porcentaje(34, 40));
        assertEquals(83.3, AsistenciaEstadisticasService.porcentaje(10, 12));
        assertEquals(78.6, AsistenciaEstadisticasService.porcentaje(11, 14));
    }

    @Test
    void sinClasesNoHayPorcentaje() {
        assertNull(AsistenciaEstadisticasService.porcentaje(0, 0));
    }

    @Test
    void semaforoUsaLosUmbralesDelClub() {
        AsistenciaUmbralesDTO umbrales = new AsistenciaUmbralesDTO();
        assertEquals("VERDE", AsistenciaEstadisticasService.nivel(80.0, umbrales));
        assertEquals("AMARILLO", AsistenciaEstadisticasService.nivel(79.9, umbrales));
        assertEquals("AMARILLO", AsistenciaEstadisticasService.nivel(60.0, umbrales));
        assertEquals("ROJO", AsistenciaEstadisticasService.nivel(59.9, umbrales));
        assertEquals("SIN_DATOS", AsistenciaEstadisticasService.nivel(null, umbrales));

        umbrales.setVerde(90);
        assertEquals("AMARILLO", AsistenciaEstadisticasService.nivel(85.0, umbrales));
    }

    @Test
    void antiguedadEnAniosYMeses() {
        LocalDate inscripcion = LocalDate.of(2024, 3, 10);
        assertEquals("2 años y 8 meses",
                AsistenciaEstadisticasService.textoAntiguedad(inscripcion, LocalDate.of(2026, 11, 10)));
        assertEquals("1 año",
                AsistenciaEstadisticasService.textoAntiguedad(inscripcion, LocalDate.of(2025, 3, 10)));
        assertEquals("1 mes",
                AsistenciaEstadisticasService.textoAntiguedad(inscripcion, LocalDate.of(2024, 4, 12)));
        assertEquals("12 días",
                AsistenciaEstadisticasService.textoAntiguedad(inscripcion, LocalDate.of(2024, 3, 22)));
        assertEquals("1 día",
                AsistenciaEstadisticasService.textoAntiguedad(inscripcion, LocalDate.of(2024, 3, 11)));
    }
}
