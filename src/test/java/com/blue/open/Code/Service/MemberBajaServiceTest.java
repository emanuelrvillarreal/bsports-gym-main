package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBajaServiceTest {

    @Mock
    private MemberAuthService memberAuthService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    private MemberBajaService service;
    private StubAuthService stubAuthService;

    @BeforeEach
    void setUp() {
        service = new MemberBajaService();
        stubAuthService = new StubAuthService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadBajaService", inscripcionActividadBajaService);
        ReflectionTestUtils.setField(service, "memberMapper", new MemberMapper());

        Company company = new Company();
        company.setId(10L);
        stubAuthService.setCompany(company);
    }

    @Test
    void darBajaLogica_pasaAInactivo() {
        Member member = member(MemberEstadoEnum.ACTIVO);
        when(memberAuthService.resolveMemberByUuid("m-uuid", 10L)).thenReturn(member);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdWithTitular(1L)).thenReturn(Optional.of(member));

        var result = service.darBajaLogica("m-uuid");

        assertEquals(MemberEstadoEnum.INACTIVO, member.getEstado());
        assertEquals(MemberEstadoEnum.INACTIVO, result.getEstado());
        verify(inscripcionActividadBajaService).bajaTodasActivasPorMember(1L, 10L);
    }

    @Test
    void darBajaLogica_idempotenteSiYaInactivo() {
        Member member = member(MemberEstadoEnum.INACTIVO);
        when(memberAuthService.resolveMemberByUuid("m-uuid", 10L)).thenReturn(member);

        service.darBajaLogica("m-uuid");

        verify(memberRepository, never()).save(any());
        verify(inscripcionActividadBajaService, never()).bajaTodasActivasPorMember(any(), any());
    }

    private Member member(MemberEstadoEnum estado) {
        Member member = new Member();
        member.setId(1L);
        member.setUuid("m-uuid");
        member.setEstado(estado);
        member.setUser(new User());
        member.setCompany(new Company());
        return member;
    }
}
