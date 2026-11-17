package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.BalanceMesDTO;
import com.blue.open.Code.Entity.DTO.BalancePaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceQueryServiceTest {

    private static final Long COMPANY_ID = 10L;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    private BalanceQueryService service;

    @BeforeEach
    void setUp() {
        service = new BalanceQueryService();
        ReflectionTestUtils.setField(service, "movimientoRepository", movimientoRepository);
        ReflectionTestUtils.setField(service, "cuotaRepository", cuotaRepository);
    }

    @Test
    void obtenerMesActual_calculaNetosYEstimados() {
        YearMonth periodo = YearMonth.now();
        when(movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                eq(COMPANY_ID), eq(MovimientoTipoEnum.INGRESO), eq(MovimientoEstadoEnum.ACTIVO),
                eq(periodo.atDay(1)), eq(periodo.atEndOfMonth())))
                .thenReturn(new BigDecimal("1000.00"));
        when(movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                eq(COMPANY_ID), eq(MovimientoTipoEnum.EGRESO), eq(MovimientoEstadoEnum.ACTIVO),
                eq(periodo.atDay(1)), eq(periodo.atEndOfMonth())))
                .thenReturn(new BigDecimal("200.00"));
        when(cuotaRepository.sumMontoPorCobrarByPeriodo(
                eq(COMPANY_ID), any(), eq(periodo.atDay(1)), eq(periodo.atEndOfMonth())))
                .thenReturn(new BigDecimal("500.00"));

        BalanceMesDTO result = service.obtenerMesActual(COMPANY_ID);

        assertEquals(0, new BigDecimal("800.00").compareTo(result.getIngresosNetos()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(result.getIngresosEstimados()));
        assertEquals(periodo.getMonthValue(), result.getMes());
    }

    @Test
    void paginarHistorico_excluyeMesActual() {
        YearMonth anterior = YearMonth.now().minusMonths(1);
        when(movimientoRepository.findDistinctPeriodosFromMovimientos(COMPANY_ID))
                .thenReturn(List.<Object[]>of(new Object[]{anterior.getYear(), anterior.getMonthValue()}));
        when(cuotaRepository.findDistinctFechasGeneracion(COMPANY_ID))
                .thenReturn(List.of());

        when(movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                eq(COMPANY_ID), eq(MovimientoTipoEnum.INGRESO), eq(MovimientoEstadoEnum.ACTIVO),
                eq(anterior.atDay(1)), eq(anterior.atEndOfMonth())))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                eq(COMPANY_ID), eq(MovimientoTipoEnum.EGRESO), eq(MovimientoEstadoEnum.ACTIVO),
                eq(anterior.atDay(1)), eq(anterior.atEndOfMonth())))
                .thenReturn(BigDecimal.ZERO);
        when(cuotaRepository.sumMontoPorCobrarByPeriodo(
                eq(COMPANY_ID), any(), eq(anterior.atDay(1)), eq(anterior.atEndOfMonth())))
                .thenReturn(BigDecimal.ZERO);

        Page<?> page = service.paginarHistorico(new BalancePaginateRequestDTO(), COMPANY_ID);

        assertEquals(1, page.getTotalElements());
        com.blue.open.Code.Entity.DTO.BalanceResumenMesDTO resumen =
                (com.blue.open.Code.Entity.DTO.BalanceResumenMesDTO) page.getContent().get(0);
        assertEquals(anterior.getMonthValue(), resumen.getMes());
        assertEquals(anterior.getYear(), resumen.getAnio());
        assertTrue(YearMonth.now().getMonthValue() != resumen.getMes()
                || YearMonth.now().getYear() != resumen.getAnio());
    }
}
