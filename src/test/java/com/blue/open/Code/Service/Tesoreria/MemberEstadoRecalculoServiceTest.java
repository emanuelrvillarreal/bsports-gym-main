package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberEstadoRecalculoServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate FECHA_CORTE = LocalDate.of(2026, 6, 11);

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    private TrackingBajaService trackingBajaService;
    private MemberEstadoRecalculoService service;

    @BeforeEach
    void setUp() {
        trackingBajaService = new TrackingBajaService();
        service = new MemberEstadoRecalculoService();
        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "cuotaRepository", cuotaRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadBajaService", trackingBajaService);

        PlatformTransactionManager txManager = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "transactionTemplate", new TransactionTemplate(txManager));

        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));
    }

    @Test
    void recalcularGrupo_diasDesdeVencimientoCumpleDiasMoroso_marcaMoroso() {
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.of(FECHA_CORTE.minusDays(31)));

        service.recalcularGrupoPorTitular(COMPANY_ID, 1L, FECHA_CORTE);

        assertEquals(MemberEstadoEnum.MOROSO, titular.getEstado());
        assertEquals(0, trackingBajaService.getCallCount());
    }

    @Test
    void recalcularGrupo_cuotaVencidaDeActividad_tambienCuentaParaMoroso() {
        // Antes las cuotas de actividad quedaban excluidas del cálculo; ahora una cuota
        // de actividad vencida cuenta igual que una social.
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.of(FECHA_CORTE.minusDays(31)));

        service.recalcularGrupoPorTitular(COMPANY_ID, 1L, FECHA_CORTE);

        assertEquals(MemberEstadoEnum.MOROSO, titular.getEstado());
    }

    @Test
    void recalcularGrupo_diasDesdeVencimientoCumpleDiasInhabilitacion_inhabilitaGrupo() {
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.of(FECHA_CORTE.minusDays(91)));

        service.recalcularGrupoPorTitular(COMPANY_ID, 1L, FECHA_CORTE);

        assertEquals(MemberEstadoEnum.INHABILITADO, titular.getEstado());
        assertEquals(1, trackingBajaService.getCallCount());
    }

    @Test
    void recalcularGrupo_sinCuotaVencida_recuperaActivo() {
        Member titular = titular(1L, MemberEstadoEnum.MOROSO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.empty());

        service.recalcularGrupoPorTitular(COMPANY_ID, 1L, FECHA_CORTE);

        assertEquals(MemberEstadoEnum.ACTIVO, titular.getEstado());
        assertNull(titular.getGrupoInhabilitadoAt());
    }

    @Test
    void recalcularGrupo_diasDesdeVencimientoBajoUmbralMoroso_mantieneActivo() {
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.of(FECHA_CORTE.minusDays(20)));

        service.recalcularGrupoPorTitular(COMPANY_ID, 1L, FECHA_CORTE);

        assertEquals(MemberEstadoEnum.ACTIVO, titular.getEstado());
    }

    @Test
    void recalcularPorCompany_soloGruposConCuotaVencida() {
        when(cuotaRepository.findTitularIdsConCuotaVencida(COMPANY_ID))
                .thenReturn(Set.of(1L));
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        when(memberRepository.findGrupoByTitularId(COMPANY_ID, 1L)).thenReturn(List.of(titular));
        when(cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(COMPANY_ID, 1L))
                .thenReturn(Optional.of(FECHA_CORTE.minusDays(31)));

        var result = service.recalcularPorCompany(COMPANY_ID, FECHA_CORTE);

        assertEquals(1, result.getGruposProcesados());
        assertEquals(MemberEstadoEnum.MOROSO, titular.getEstado());
    }

    private Member titular(Long id, MemberEstadoEnum estado) {
        Member member = new Member();
        member.setId(id);
        member.setCompany(company());
        member.setEstado(estado);
        return member;
    }

    private Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setDiasVencimientoMoroso(31);
        company.setDiasInhabilitacion(91);
        return company;
    }

    private static class TrackingBajaService extends InscripcionActividadBajaService {

        private int callCount;

        @Override
        public void bajaTodasActivasDelGrupo(List<Member> grupo, Long companyId) {
            callCount++;
        }

        int getCallCount() {
            return callCount;
        }
    }
}
