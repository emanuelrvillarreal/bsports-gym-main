package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCuotaSocialRangoRecalculoServiceTest {

    private static final Long COMPANY_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    private MemberCuotaSocialRangoRecalculoService service;

    @BeforeEach
    void setUp() {
        service = new MemberCuotaSocialRangoRecalculoService();
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "cuotaSocialRangoRepository", cuotaSocialRangoRepository);
    }

    @Test
    void recalcularPorCompany_socioSinRangoConMatch_asignaElRango() {
        Member member = miembro(20, null);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(COMPANY_ID)).thenReturn(List.of(member));

        CuotaSocialRango rango = rango(30L);
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.of(rango));

        service.recalcularPorCompany(COMPANY_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Member>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(rango, captor.getValue().get(0).getCuotaSocialRango());
    }

    @Test
    void recalcularPorCompany_socioEnStatusPendienteAprobacion_tambienSeRecalcula() {
        Member member = miembro(20, null);
        member.setStatus(-2);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(COMPANY_ID)).thenReturn(List.of(member));

        CuotaSocialRango rango = rango(30L);
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.of(rango));

        service.recalcularPorCompany(COMPANY_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Member>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(rango, captor.getValue().get(0).getCuotaSocialRango());
    }

    @Test
    void recalcularPorCompany_socioYaTieneElRangoCorrecto_noLoModifica() {
        CuotaSocialRango rango = rango(30L);
        Member member = miembro(20, rango);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(COMPANY_ID)).thenReturn(List.of(member));
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.of(rango));

        service.recalcularPorCompany(COMPANY_ID);

        verify(memberRepository, never()).saveAll(anyList());
    }

    @Test
    void recalcularPorCompany_sinFechaNacimiento_seOmite() {
        Member member = new Member();
        member.setId(1L);
        User user = new User();
        user.setFechaNacimiento(null);
        member.setUser(user);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(COMPANY_ID)).thenReturn(List.of(member));

        service.recalcularPorCompany(COMPANY_ID);

        verify(memberRepository, never()).saveAll(anyList());
        verify(cuotaSocialRangoRepository, never()).findByCompanyIdAndEdadHasta(anyLong(), anyInt());
    }

    @Test
    void recalcularPorCompany_sinRangoQueMatchee_noTocaLaAsignacionExistente() {
        CuotaSocialRango rangoActual = rango(30L);
        Member member = miembro(150, rangoActual);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(COMPANY_ID)).thenReturn(List.of(member));
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());

        service.recalcularPorCompany(COMPANY_ID);

        verify(memberRepository, never()).saveAll(anyList());
        assertEquals(rangoActual, member.getCuotaSocialRango());
    }

    @Test
    void recalcularPorRangoBorrado_reasignaAlNuevoRangoQueCorresponde() {
        CuotaSocialRango rangoBorrado = rango(10L);
        Member member = miembro(20, rangoBorrado);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCuotaSocialRangoIdAndCompanyIdWithUser(10L, COMPANY_ID))
                .thenReturn(List.of(member));

        CuotaSocialRango nuevoRango = rango(30L);
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.of(nuevoRango));

        service.recalcularPorRangoBorrado(COMPANY_ID, 10L);

        assertEquals(nuevoRango, member.getCuotaSocialRango());
        verify(memberRepository).saveAll(List.of(member));
    }

    @Test
    void recalcularPorRangoBorrado_sinNuevoMatch_dejaAlSocioSinRango() {
        CuotaSocialRango rangoBorrado = rango(10L);
        Member member = miembro(150, rangoBorrado);
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCuotaSocialRangoIdAndCompanyIdWithUser(10L, COMPANY_ID))
                .thenReturn(List.of(member));
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(COMPANY_ID), anyInt()))
                .thenReturn(Optional.empty());

        service.recalcularPorRangoBorrado(COMPANY_ID, 10L);

        assertEquals(null, member.getCuotaSocialRango());
        verify(memberRepository).saveAll(List.of(member));
    }

    @Test
    void recalcularPorRangoBorrado_sinSociosAfectados_noLlamaSaveAll() {
        when(memberRepository.findParaRecalculoCuotaSocialRangoByCuotaSocialRangoIdAndCompanyIdWithUser(10L, COMPANY_ID))
                .thenReturn(List.of());

        service.recalcularPorRangoBorrado(COMPANY_ID, 10L);

        verify(memberRepository, never()).saveAll(anyList());
    }

    private Member miembro(int edad, CuotaSocialRango rangoActual) {
        Member member = new Member();
        member.setId(1L);
        User user = new User();
        user.setFechaNacimiento(LocalDate.now().minusYears(edad));
        member.setUser(user);
        member.setCuotaSocialRango(rangoActual);
        return member;
    }

    private CuotaSocialRango rango(Long id) {
        CuotaSocialRango rango = new CuotaSocialRango();
        rango.setId(id);
        return rango;
    }
}
