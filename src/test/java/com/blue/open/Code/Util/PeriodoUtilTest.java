package com.blue.open.Code.Util;

import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodoUtilTest {

    @Test
    void parseOrThrow_valido() {
        YearMonth ym = PeriodoUtil.parseOrThrow("06-2026");
        assertEquals(2026, ym.getYear());
        assertEquals(6, ym.getMonthValue());
        assertEquals("06-2026", PeriodoUtil.format(ym));
    }

    @Test
    void parseOrThrow_invalido() {
        CustomException ex = assertThrows(CustomException.class, () -> PeriodoUtil.parseOrThrow("2026-06"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
