package com.blue.open.Code.Util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaEdadUtilTest {

    private static final int ANIO_2026 = 2026;
    private static final int EDAD_MIN = 23;
    private static final int EDAD_MAX = 25;

    @Test
    void edadMaximaReglamentaria_nacido26Oct2002_cuentaComo24En2026() {
        LocalDate nacimiento = LocalDate.of(2002, 10, 26);

        assertEquals(24, CategoriaEdadUtil.edadMaximaReglamentaria(nacimiento, ANIO_2026));
    }

    @Test
    void cumpleCategoria_nacido26Oct2002_entraEn23a25() {
        LocalDate nacimiento = LocalDate.of(2002, 10, 26);

        assertTrue(CategoriaEdadUtil.cumpleEdadMinima(nacimiento, EDAD_MIN, ANIO_2026));
        assertTrue(CategoriaEdadUtil.cumpleCategoria(nacimiento, EDAD_MIN, EDAD_MAX, ANIO_2026));
    }

    @Test
    void cumpleCategoria_nacido15Ene2003_noCumpleMinima23En2026() {
        LocalDate nacimiento = LocalDate.of(2003, 1, 15);

        assertEquals(23, CategoriaEdadUtil.edadMaximaReglamentaria(nacimiento, ANIO_2026));
        assertFalse(CategoriaEdadUtil.cumpleEdadMinima(nacimiento, EDAD_MIN, ANIO_2026));
        assertFalse(CategoriaEdadUtil.cumpleCategoria(nacimiento, EDAD_MIN, EDAD_MAX, ANIO_2026));
    }

    @Test
    void cumpleCategoria_nacido2000_superaEdadMaxima25() {
        LocalDate nacimiento = LocalDate.of(2000, 5, 10);

        assertEquals(26, CategoriaEdadUtil.edadMaximaReglamentaria(nacimiento, ANIO_2026));
        assertFalse(CategoriaEdadUtil.cumpleCategoria(nacimiento, EDAD_MIN, EDAD_MAX, ANIO_2026));
    }

    @Test
    void cumpleCategoria_fechaNacimientoNull_retornaFalse() {
        assertFalse(CategoriaEdadUtil.cumpleCategoria(null, EDAD_MIN, EDAD_MAX, ANIO_2026));
        assertFalse(CategoriaEdadUtil.cumpleEdadMinima(null, EDAD_MIN, ANIO_2026));
    }
}
