package com.blue.open.Code.Event;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.DTO.MemberEstadoRecalculoResultDTO;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Service.Tesoreria.MemberEstadoRecalculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotasVencidasProcesadasEventListenerTest {

    @Mock
    private CompanyRepository companyRepository;

    private CapturingRecalcService capturingRecalc;
    private CuotasVencidasProcesadasEventListener listener;

    @BeforeEach
    void setUp() {
        capturingRecalc = new CapturingRecalcService();
        listener = new CuotasVencidasProcesadasEventListener();
        ReflectionTestUtils.setField(listener, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(listener, "memberEstadoRecalculoService", capturingRecalc);
    }

    @Test
    void onCuotasVencidasProcesadas_recalculaPorCadaClubActivo() {
        LocalDate fechaCorte = LocalDate.of(2026, 6, 11);
        Company company1 = new Company();
        company1.setId(1L);
        Company company2 = new Company();
        company2.setId(2L);
        when(companyRepository.findByStatus(0)).thenReturn(List.of(company1, company2));

        listener.onCuotasVencidasProcesadas(new CuotasVencidasProcesadasEvent(fechaCorte, 2, 5));

        assertEquals(List.of(1L, 2L), capturingRecalc.getCompanyIds());
        assertEquals(fechaCorte, capturingRecalc.getLastFechaCorte());
    }

    private static class CapturingRecalcService extends MemberEstadoRecalculoService {

        private final List<Long> companyIds = new ArrayList<>();
        private LocalDate lastFechaCorte;

        @Override
        public MemberEstadoRecalculoResultDTO recalcularPorCompany(Long companyId, LocalDate fechaCorte) {
            companyIds.add(companyId);
            lastFechaCorte = fechaCorte;
            return new MemberEstadoRecalculoResultDTO();
        }

        List<Long> getCompanyIds() {
            return companyIds;
        }

        LocalDate getLastFechaCorte() {
            return lastFechaCorte;
        }
    }
}
