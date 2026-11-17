package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.ReporteEconomicoRequestDTO;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Reglas puras del reporte economico: variacion entre periodos, textos y periodo del archivo. */
class ReporteEconomicoReglasTest {

    @Test
    void variacionPorcentualEntrePeriodos() {
        assertEquals(25.0, ReporteEconomicoQueryService.variacion(new BigDecimal("1000"), new BigDecimal("1250")));
        assertEquals(-12.5, ReporteEconomicoQueryService.variacion(new BigDecimal("800"), new BigDecimal("700")));
        // * Balance negativo que mejora: la variacion es positiva
        assertEquals(50.0, ReporteEconomicoQueryService.variacion(new BigDecimal("-200"), new BigDecimal("-100")));
    }

    @Test
    void sinBaseNoHayVariacion() {
        assertNull(ReporteEconomicoQueryService.variacion(BigDecimal.ZERO, new BigDecimal("500")));
    }

    @Test
    void textosDeCategoriaYMedioDePago() {
        assertEquals("Cuotas sociales", ReporteEconomicoQueryService.textoCategoria("CUOTA_SOCIAL"));
        assertEquals("Cuotas de actividades", ReporteEconomicoQueryService.textoCategoria("CUOTA_ACTIVIDAD"));
        assertEquals("Gastos y otros egresos", ReporteEconomicoQueryService.textoCategoria("GASTOS"));
        assertEquals("Otros ingresos", ReporteEconomicoQueryService.textoCategoria("MANUAL"));
        assertEquals("Mercado Pago", ReporteEconomicoQueryService.textoMedioPago(MedioPagoEnum.MERCADO_PAGO));
        assertEquals("Sin medio de pago", ReporteEconomicoQueryService.textoMedioPago(null));
    }

    @Test
    void periodoDelEncabezado() {
        ReporteEconomicoRequestDTO request = new ReporteEconomicoRequestDTO();
        assertEquals("Todo el historial", ReporteEconomicoExcelService.textoPeriodo(request));
        request.setDesde(LocalDate.of(2026, 3, 1));
        request.setHasta(LocalDate.of(2026, 3, 31));
        assertEquals("01/03/2026 al 31/03/2026", ReporteEconomicoExcelService.textoPeriodo(request));
    }

    @Test
    void copiarConservaTodosLosFiltros() {
        ReporteEconomicoRequestDTO original = new ReporteEconomicoRequestDTO();
        original.setActividadUuid("act");
        original.setConcepto("cuota");
        original.setImporteMinimo(new BigDecimal("10"));
        ReporteEconomicoRequestDTO copia = ReporteEconomicoQueryService.copiar(original);
        assertEquals("act", copia.getActividadUuid());
        assertEquals("cuota", copia.getConcepto());
        assertEquals(new BigDecimal("10"), copia.getImporteMinimo());
    }
}
