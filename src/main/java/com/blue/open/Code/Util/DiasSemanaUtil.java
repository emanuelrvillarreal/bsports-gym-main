package com.blue.open.Code.Util;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DiasSemanaUtil {

    private DiasSemanaUtil() {
    }

    public static List<DiaSemanaEnum> parseAndValidate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Días de semana requeridos",
                    "DiasSemanaUtil | dias_semana vacío");
        }
        Set<DiaSemanaEnum> dias = new LinkedHashSet<>();
        for (String token : raw.split(";")) {
            String normalized = token == null ? "" : token.trim().toUpperCase();
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                dias.add(DiaSemanaEnum.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Día de semana inválido: " + normalized,
                        "DiasSemanaUtil | token: " + token);
            }
        }
        if (dias.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Al menos un día de semana es requerido",
                    "DiasSemanaUtil | sin tokens válidos");
        }
        return new ArrayList<>(dias);
    }

    public static String toStorage(List<DiaSemanaEnum> dias) {
        if (dias == null || dias.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Días de semana requeridos",
                    "DiasSemanaUtil | lista vacía");
        }
        return dias.stream()
                .map(Enum::name)
                .collect(Collectors.joining(";"));
    }

    public static String normalizeStorage(String raw) {
        return toStorage(parseAndValidate(raw));
    }

    /**
     * Primera fecha &gt;= {@code desde} que cae en el {@code diaSemana} indicado.
     */
    public static LocalDate primeraOcurrenciaDesde(DayOfWeek diaSemana, LocalDate desde) {
        if (diaSemana == null || desde == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Parámetros requeridos",
                    "DiasSemanaUtil | diaSemana o desde null");
        }
        int diasHasta = (diaSemana.getValue() - desde.getDayOfWeek().getValue() + 7) % 7;
        return desde.plusDays(diasHasta);
    }

    public static boolean matchesDay(String diasSemanaStorage, DayOfWeek dayOfWeek) {
        EnumSet<DayOfWeek> configured = EnumSet.noneOf(DayOfWeek.class);
        for (DiaSemanaEnum dia : parseAndValidate(diasSemanaStorage)) {
            configured.add(dia.toDayOfWeek());
        }
        return configured.contains(dayOfWeek);
    }
}
