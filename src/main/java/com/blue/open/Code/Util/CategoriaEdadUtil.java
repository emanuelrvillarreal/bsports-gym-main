package com.blue.open.Code.Util;

import java.time.LocalDate;

public final class CategoriaEdadUtil {

    private CategoriaEdadUtil() {
    }

    public static int anioReferenciaActual() {
        return LocalDate.now().getYear();
    }

    /**
     * Edad reglamentaria maxima: anio de referencia menos anio de nacimiento (anio cumplido deportivo).
     */
    public static int edadMaximaReglamentaria(LocalDate fechaNacimiento, int anioReferencia) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("fechaNacimiento requerida");
        }
        return anioReferencia - fechaNacimiento.getYear();
    }

    /**
     * Cumplio la edad minima en el anio anterior al de referencia o antes.
     */
    public static boolean cumpleEdadMinima(LocalDate fechaNacimiento, int edadMinima, int anioReferencia) {
        if (fechaNacimiento == null) {
            return false;
        }
        return fechaNacimiento.plusYears(edadMinima).getYear() <= anioReferencia - 1;
    }

    public static boolean cumpleCategoria(
            LocalDate fechaNacimiento,
            int edadMinima,
            int edadMaxima,
            int anioReferencia) {
        if (fechaNacimiento == null) {
            return false;
        }
        int edadMaxReglamentaria = edadMaximaReglamentaria(fechaNacimiento, anioReferencia);
        return edadMaxReglamentaria <= edadMaxima
                && cumpleEdadMinima(fechaNacimiento, edadMinima, anioReferencia);
    }
}
