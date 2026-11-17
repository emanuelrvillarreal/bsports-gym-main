package com.blue.open.Code.Service.Equipamiento;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Alerta de mantenimiento: vencido si ya pasó, próximo si cae en los próximos 30 días. */
class EquipamientoReglasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 5);

    @Test
    void vencidoSiLaFechaYaPaso() {
        assertEquals("VENCIDO", EquipamientoService.alerta(HOY.minusDays(1), HOY));
    }

    @Test
    void proximoDentroDeLos30Dias() {
        assertEquals("PROXIMO", EquipamientoService.alerta(HOY, HOY));
        assertEquals("PROXIMO", EquipamientoService.alerta(HOY.plusDays(30), HOY));
    }

    @Test
    void sinAlertaSiFaltaMasOQueNoHayFecha() {
        assertNull(EquipamientoService.alerta(HOY.plusDays(31), HOY));
        assertNull(EquipamientoService.alerta(null, HOY));
    }
}
