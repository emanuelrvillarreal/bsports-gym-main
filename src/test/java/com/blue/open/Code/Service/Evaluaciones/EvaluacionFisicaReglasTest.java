package com.blue.open.Code.Service.Evaluaciones;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** El IMC se calcula siempre igual: peso / altura en metros al cuadrado, con dos decimales. */
class EvaluacionFisicaReglasTest {

    @Test
    void calculaElImcConDosDecimales() {
        assertEquals(new BigDecimal("22.86"), EvaluacionFisicaService.imc(new BigDecimal("70"), new BigDecimal("175")));
        assertEquals(new BigDecimal("24.69"), EvaluacionFisicaService.imc(new BigDecimal("80"), new BigDecimal("180")));
        assertEquals(new BigDecimal("15.63"), EvaluacionFisicaService.imc(new BigDecimal("40"), new BigDecimal("160")));
    }

    @Test
    void sinPesoOAlturaNoHayImc() {
        assertNull(EvaluacionFisicaService.imc(null, new BigDecimal("175")));
        assertNull(EvaluacionFisicaService.imc(new BigDecimal("70"), null));
        assertNull(EvaluacionFisicaService.imc(new BigDecimal("70"), BigDecimal.ZERO));
    }
}
