package com.blue.open.Code.Util;

import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class PeriodoUtil {

    private static final DateTimeFormatter PERIODO_FORMAT =
            DateTimeFormatter.ofPattern("MM-yyyy", Locale.ROOT);

    private PeriodoUtil() {
    }

    public static YearMonth parseOrThrow(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Período requerido",
                    "PeriodoUtil | periodo vacío");
        }
        try {
            return YearMonth.parse(periodo.trim(), PERIODO_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Período inválido. Use formato MM-yyyy",
                    "PeriodoUtil | periodo: " + periodo);
        }
    }

    public static String format(YearMonth yearMonth) {
        return yearMonth.format(PERIODO_FORMAT);
    }
}
