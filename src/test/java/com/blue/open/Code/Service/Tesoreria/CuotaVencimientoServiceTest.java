package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Event.CuotasVencidasProcesadasEvent;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaVencimientoServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CuotaVencimientoService cuotaVencimientoService;

    @BeforeEach
    void setUp() {
        cuotaVencimientoService = new CuotaVencimientoService();
        ReflectionTestUtils.setField(cuotaVencimientoService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(cuotaVencimientoService, "cuotaRepository", cuotaRepository);
        ReflectionTestUtils.setField(cuotaVencimientoService, "eventPublisher", eventPublisher);
    }

    @Test
    void marcarVencidasPorCompany_actualizaPendientesVencidasInclusive() {
        LocalDate fechaCorte = LocalDate.of(2026, 6, 11);
        when(cuotaRepository.marcarPendientesVencidas(
                1L, CuotaEstadoEnum.PENDIENTE, CuotaEstadoEnum.VENCIDA, fechaCorte))
                .thenReturn(5);

        int marcadas = cuotaVencimientoService.marcarVencidasPorCompany(1L, fechaCorte);

        assertEquals(5, marcadas);
        verify(cuotaRepository).marcarPendientesVencidas(
                eq(1L), eq(CuotaEstadoEnum.PENDIENTE), eq(CuotaEstadoEnum.VENCIDA), eq(fechaCorte));
    }

    @Test
    void marcarVencidasGlobal_publicaEventoSiempre() {
        LocalDate fechaCorte = LocalDate.of(2026, 6, 11);
        Company company1 = new Company();
        company1.setId(1L);
        Company company2 = new Company();
        company2.setId(2L);
        when(companyRepository.findByStatus(0)).thenReturn(List.of(company1, company2));
        when(cuotaRepository.marcarPendientesVencidas(
                org.mockito.ArgumentMatchers.anyLong(),
                eq(CuotaEstadoEnum.PENDIENTE),
                eq(CuotaEstadoEnum.VENCIDA),
                eq(fechaCorte)))
                .thenReturn(0, 0);

        var result = cuotaVencimientoService.marcarVencidasGlobal(fechaCorte);

        assertEquals(0, result.getCuotasMarcadasVencidas());

        ArgumentCaptor<CuotasVencidasProcesadasEvent> captor =
                ArgumentCaptor.forClass(CuotasVencidasProcesadasEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(fechaCorte, captor.getValue().getFechaCorte());
        assertEquals(2, captor.getValue().getCompaniesProcessed());
    }
}
