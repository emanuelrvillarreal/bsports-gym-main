package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberPortalProfileCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberPortalProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CertificadoMedicoQueryService certificadoMedicoQueryService;

    private MemberPortalProfileService service;
    private MemberAuthService memberAuthService;
    private StubAuthService stubAuthService;
    private Company company;
    private User sessionUser;

    @BeforeEach
    void setUp() {
        service = new MemberPortalProfileService();
        stubAuthService = new StubAuthService();
        memberAuthService = new MemberAuthService();
        ReflectionTestUtils.setField(memberAuthService, "authService", stubAuthService);
        ReflectionTestUtils.setField(memberAuthService, "memberRepository", memberRepository);

        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "certificadoMedicoQueryService", certificadoMedicoQueryService);
        MemberMapper memberMapper = new MemberMapper();
        ReflectionTestUtils.setField(memberMapper, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);

        company = new Company();
        company.setId(10L);
        stubAuthService.setCompany(company);

        sessionUser = new User();
        sessionUser.setId(99L);
        stubAuthService.setUser(sessionUser);
    }

    @Test
    void obtenerPerfil_ok() {
        Member member = activeMember();
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(member));

        var result = service.obtenerPerfil();

        assertEquals("member-uuid", result.getMemberUuid());
        assertEquals("foto.jpg", result.getImageUrl());
    }

    @Test
    void actualizarPerfil_noModificaImageUrl() {
        Member member = activeMember();
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(member));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdWithTitular(1L)).thenReturn(Optional.of(member));

        MemberPortalProfileCommand command = new MemberPortalProfileCommand();
        command.setNombre("Nuevo");

        service.actualizarPerfil(command, null);

        assertEquals("foto.jpg", member.getUser().getImageUrl());
        assertEquals("Nuevo", member.getUser().getNombre());
    }

    @Test
    void actualizarPerfil_titularModificaFamiliar() {
        Member titular = activeMember();
        Member familiar = activeMember();
        familiar.setId(2L);
        familiar.setUuid("familiar-uuid");
        familiar.setTitular(titular);

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(titular));
        when(memberRepository.findByUuidAndCompanyIdWithDetails("familiar-uuid", 10L))
                .thenReturn(Optional.of(familiar));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdWithTitular(2L)).thenReturn(Optional.of(familiar));

        MemberPortalProfileCommand command = new MemberPortalProfileCommand();
        command.setNombre("Hijo");

        service.actualizarPerfil(command, "familiar-uuid");

        assertEquals("Hijo", familiar.getUser().getNombre());
        assertEquals("Juan", titular.getUser().getNombre());
    }

    @Test
    void actualizarPerfil_rechazaSiNoEsTitular() {
        Member titular = activeMember();
        Member current = activeMember();
        current.setId(2L);
        current.setUuid("familiar-uuid");
        current.setTitular(titular);

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(current));

        MemberPortalProfileCommand command = new MemberPortalProfileCommand();

        CustomException ex = assertThrows(CustomException.class,
                () -> service.actualizarPerfil(command, "otro-uuid"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void actualizarPerfil_rechazaMemberDeOtroGrupo() {
        Member titular = activeMember();
        Member ajeno = activeMember();
        ajeno.setId(3L);
        ajeno.setUuid("ajeno-uuid");

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(titular));
        when(memberRepository.findByUuidAndCompanyIdWithDetails("ajeno-uuid", 10L))
                .thenReturn(Optional.of(ajeno));

        MemberPortalProfileCommand command = new MemberPortalProfileCommand();

        CustomException ex = assertThrows(CustomException.class,
                () -> service.actualizarPerfil(command, "ajeno-uuid"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void obtenerPerfil_rechazaInactivo() {
        Member member = activeMember();
        member.setEstado(MemberEstadoEnum.INACTIVO);
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(member));

        CustomException ex = assertThrows(CustomException.class, () -> service.obtenerPerfil());

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private Member activeMember() {
        User user = new User();
        user.setNombre("Juan");
        user.setApellido("Pérez");
        user.setImageUrl("foto.jpg");

        Member member = new Member();
        member.setId(1L);
        member.setUuid("member-uuid");
        member.setEstado(MemberEstadoEnum.ACTIVO);
        member.setUser(user);
        member.setCompany(company);
        return member;
    }
}
