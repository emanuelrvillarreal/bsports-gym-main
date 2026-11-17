package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalFamiliarAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoResolver;
import com.blue.open.Code.Service.support.NoOpMemberRegistroNotificacionService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
class MemberPortalFamiliarServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MemberPortalFamiliarService portalService;
    private MemberAuthService memberAuthService;
    private StubAuthService stubAuthService;
    private Company company;
    private Member titular;
    private User titularUser;

    @BeforeEach
    void setUp() {
        MemberMapper memberMapper = new MemberMapper();
        ReflectionTestUtils.setField(memberMapper, "memberRepository", memberRepository);
        MemberUserValidationHelper validationHelper = new MemberUserValidationHelper();
        ReflectionTestUtils.setField(validationHelper, "userRepository", userRepository);

        MemberUserProvisioningService provisioningService = new MemberUserProvisioningService();
        ReflectionTestUtils.setField(provisioningService, "userRepository", userRepository);
        ReflectionTestUtils.setField(provisioningService, "userCompanyRoleRepository", userCompanyRoleRepository);
        ReflectionTestUtils.setField(provisioningService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(provisioningService, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(provisioningService, "validationHelper", validationHelper);
        ReflectionTestUtils.setField(provisioningService, "passwordEncoder", passwordEncoder);

        MemberFamiliarService familiarService = new MemberFamiliarService();
        stubAuthService = new StubAuthService();
        ReflectionTestUtils.setField(familiarService, "authService", stubAuthService);
        ReflectionTestUtils.setField(familiarService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(familiarService, "userProvisioningService", provisioningService);
        ReflectionTestUtils.setField(familiarService, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(familiarService, "registroNotificacionService",
                new NoOpMemberRegistroNotificacionService());
        ReflectionTestUtils.setField(familiarService, "cuotaSocialRangoResolver", new CuotaSocialRangoResolver());

        MemberNumeroSocioService numeroSocioService = new MemberNumeroSocioService();
        ReflectionTestUtils.setField(numeroSocioService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(familiarService, "numeroSocioService", numeroSocioService);

        memberAuthService = new MemberAuthService();
        ReflectionTestUtils.setField(memberAuthService, "authService", stubAuthService);
        ReflectionTestUtils.setField(memberAuthService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(familiarService, "memberAuthService", memberAuthService);

        portalService = new MemberPortalFamiliarService();
        ReflectionTestUtils.setField(portalService, "authService", stubAuthService);
        ReflectionTestUtils.setField(portalService, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(portalService, "memberFamiliarService", familiarService);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        stubAuthService.setCompany(company);

        titularUser = new User();
        titularUser.setId(1L);
        titularUser.setUuid("titular-user-uuid");
        stubAuthService.setUser(titularUser);

        titular = new Member();
        titular.setId(100L);
        titular.setUuid("titular-uuid");
        titular.setCompany(company);
        titular.setUser(titularUser);
        titular.setEstado(MemberEstadoEnum.ACTIVO);
    }

    @Test
    void altaFamiliar_usuarioNuevo_conPassword() {
        MemberPortalFamiliarAltaCommand command = portalCommand("nuevo@fam.com", "secret123");

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(titular));
        when(userRepository.findFirstByEmailAndStatusNot(eq("nuevo@fam.com"), eq(-1))).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.FAMILIAR.name(), company)).thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, u.getStatus());
            u.setId(60L);
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(60L), anyCollection()))
                .thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(5L);
            m.setUuid("fam-uuid");
            m.setTitular(titular);
            m.setParentesco(ParentescoEnum.HIJO);
            m.setCompany(company);
            m.setUser(new User());
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, m.getStatus());
            return m;
        });
        when(memberRepository.findByIdWithTitular(5L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(5L);
            m.setUuid("fam-uuid");
            m.setTitular(titular);
            m.setParentesco(ParentescoEnum.HIJO);
            return Optional.of(m);
        });

        MemberDTO result = portalService.altaFamiliar(command);

        assertEquals("fam-uuid", result.getMemberUuid());
        assertEquals(ParentescoEnum.HIJO, result.getParentesco());
    }

    @Test
    void altaFamiliar_usuarioExistente_sinPassword() {
        MemberPortalFamiliarAltaCommand command = portalCommand("existente@fam.com", null);

        User existing = new User();
        existing.setId(61L);
        existing.setUuid("existing-uuid");
        existing.setDni("30111222");
        existing.setUserCompanyRoles(new ArrayList<>());

        command.setDni("30111222");

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(titular));
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "existente@fam.com")).thenReturn(existing);
        when(userRepository.findFirstByUuidWithDetails("existing-uuid")).thenReturn(existing);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.FAMILIAR.name(), company)).thenReturn(new Role());
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(existing);
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(61L), anyCollection()))
                .thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(6L);
            m.setUuid("fam-2");
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, m.getStatus());
            return m;
        });
        when(memberRepository.findByIdWithTitular(6L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(6L);
            m.setUuid("fam-2");
            m.setParentesco(ParentescoEnum.HIJO);
            return Optional.of(m);
        });

        MemberDTO result = portalService.altaFamiliar(command);

        assertEquals("fam-2", result.getMemberUuid());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void altaFamiliar_rechazaSiLogueadoEsFamiliar() {
        Member familiar = new Member();
        familiar.setId(2L);
        familiar.setTitular(titular);

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(familiar));

        CustomException ex = assertThrows(CustomException.class,
                () -> portalService.altaFamiliar(portalCommand("a@b.com", "pwd")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void altaFamiliar_rechazaTitularInhabilitado() {
        titular.setEstado(MemberEstadoEnum.INHABILITADO);
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(titular));

        CustomException ex = assertThrows(CustomException.class,
                () -> portalService.altaFamiliar(portalCommand("a@b.com", "pwd")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void altaFamiliar_yaEsMemberDelClub_devuelve409() {
        User existing = new User();
        existing.setId(70L);
        existing.setUuid("u70");
        existing.setDni("30111222");
        existing.setUserCompanyRoles(new ArrayList<>());

        MemberPortalFamiliarAltaCommand command = portalCommand("dup@fam.com", null);
        command.setDni("30111222");

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(titular));
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "dup@fam.com")).thenReturn(existing);
        when(userRepository.findFirstByUuidWithDetails("u70")).thenReturn(existing);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.FAMILIAR.name(), company)).thenReturn(new Role());
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(70L), anyCollection()))
                .thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> portalService.altaFamiliar(command));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void altaFamiliar_usuarioNuevo_sinPassword_devuelve400() {
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(1L, 10L)).thenReturn(Optional.of(titular));

        CustomException ex = assertThrows(CustomException.class,
                () -> portalService.altaFamiliar(portalCommand("nuevo@fam.com", null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    private MemberPortalFamiliarAltaCommand portalCommand(String email, String password) {
        MemberPortalFamiliarAltaCommand command = new MemberPortalFamiliarAltaCommand();
        command.setNombre("Ana");
        command.setApellido("Pérez");
        command.setEmail(email);
        command.setPassword(password);
        command.setParentesco(ParentescoEnum.HIJO);
        return command;
    }
}
