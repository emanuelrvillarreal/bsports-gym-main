package com.blue.open.Code.Util;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiasSemanaUtilTest {

    @Test
    void parseAndValidate_aceptaTokensSeparadosPorPuntoYComa() {
        List<DiaSemanaEnum> dias = DiasSemanaUtil.parseAndValidate("LUNES;MIERCOLES;VIERNES");
        assertEquals(3, dias.size());
        assertEquals(DiaSemanaEnum.LUNES, dias.get(0));
        assertEquals(DiaSemanaEnum.MIERCOLES, dias.get(1));
    }

    @Test
    void toStorage_concatenaConSeparador() {
        assertEquals("LUNES;MIERCOLES", DiasSemanaUtil.toStorage(List.of(DiaSemanaEnum.LUNES, DiaSemanaEnum.MIERCOLES)));
    }

    @Test
    void normalizeStorage_normalizaMayusculas() {
        assertEquals("LUNES;VIERNES", DiasSemanaUtil.normalizeStorage(" lunes ; viernes "));
    }

    @Test
    void matchesDay_respondeSegunDiaConfigurado() {
        assertTrue(DiasSemanaUtil.matchesDay("LUNES;MIERCOLES", DayOfWeek.MONDAY));
        assertFalse(DiasSemanaUtil.matchesDay("LUNES;MIERCOLES", DayOfWeek.FRIDAY));
    }

    @Test
    void primeraOcurrenciaDesde_encuentraElProximoDiaDeLaSemana() {
        LocalDate martes = LocalDate.of(2026, 6, 2);
        assertEquals(LocalDate.of(2026, 6, 8), DiasSemanaUtil.primeraOcurrenciaDesde(DayOfWeek.MONDAY, martes));
        assertEquals(martes, DiasSemanaUtil.primeraOcurrenciaDesde(DayOfWeek.TUESDAY, martes));
    }

    @Test
    void parseAndValidate_rechazaTokenInvalido() {
        assertThrows(CustomException.class, () -> DiasSemanaUtil.parseAndValidate("LUNES;FOO"));
    }
}
