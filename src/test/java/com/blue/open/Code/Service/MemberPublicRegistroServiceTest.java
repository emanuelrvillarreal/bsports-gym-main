package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPublicRegistroCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoResolver;
import com.blue.open.Code.Service.support.RecordingCuotaSocialGeneracionService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
class MemberPublicRegistroServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CompanyAccesoService companyAccesoService;

    private RecordingCuotaSocialGeneracionService cuotaSocialGeneracionService;
    private MemberPublicRegistroService publicService;
    private Company company;
    private Role socioTitularRole;

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

        MemberNumeroSocioService numeroSocioService = new MemberNumeroSocioService();
        ReflectionTestUtils.setField(numeroSocioService, "memberRepository", memberRepository);

        cuotaSocialGeneracionService = new RecordingCuotaSocialGeneracionService();


        MemberTitularService titularService = new MemberTitularService();
        ReflectionTestUtils.setField(titularService, "authService", new StubAuthService());
        ReflectionTestUtils.setField(titularService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(titularService, "cuotaSocialGeneracionService", cuotaSocialGeneracionService);
        ReflectionTestUtils.setField(titularService, "userProvisioningService", provisioningService);
        ReflectionTestUtils.setField(titularService, "numeroSocioService", numeroSocioService);
        ReflectionTestUtils.setField(titularService, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(titularService, "registroNotificacionService",
                new NoOpMemberRegistroNotificacionService());
        ReflectionTestUtils.setField(titularService, "cuotaSocialRangoResolver", new CuotaSocialRangoResolver());

        StubAuthService stubAuthService = new StubAuthService() {
            @Override
            public String resolveCompanyUuid(String companyUuid) {
                return companyUuid;
            }
        };

        publicService = new MemberPublicRegistroService();
        ReflectionTestUtils.setField(publicService, "authService", stubAuthService);
        ReflectionTestUtils.setField(publicService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(publicService, "memberTitularService", titularService);
        ReflectionTestUtils.setField(publicService, "companyAccesoService", companyAccesoService);

        // Club sin bloquear por defecto: lenient() porque los tests que cortan antes de resolver
        // la company (uuid invalido, sin header) no llegan a consultarlo.
        lenient().when(companyAccesoService.estaBloqueada(any(Company.class))).thenReturn(false);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        company.setCuotaSocialHabilitada(false);

        socioTitularRole = new Role();
        socioTitularRole.setName(RoleEnum.SOCIO_TITULAR.name());
    }

    @Test
    void registroTitular_usuarioNuevo_conPassword() {
        MemberPublicRegistroCommand command = publicCommand("nuevo@test.com", "secret123");

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(userRepository.findFirstByEmailAndStatusNot(eq("nuevo@test.com"), eq(-1))).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company)).thenReturn(socioTitularRole);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, u.getStatus());
            u.setId(50L);
            u.setUuid("user-uuid");
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(50L), anyCollection()))
                .thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(1L);
            m.setUuid("member-uuid");
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, m.getStatus());
            assertEquals(null, m.getNumeroSocio());
            return m;
        });
        when(memberRepository.findByIdWithTitular(1L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(1L);
            m.setUuid("member-uuid");
            m.setCompany(company);
            m.setEstado(MemberEstadoEnum.ACTIVO);
            return Optional.of(m);
        });

        MemberDTO result = publicService.registroTitular("club-uuid", command);

        assertEquals("member-uuid", result.getMemberUuid());
        assertEquals(null, result.getNumeroSocio());
        // El registro público queda PENDING_APPROVAL: la cuota social se genera recién
        // al aprobar (MemberRegistroAprobacionService), no en el alta.
        assertFalse(cuotaSocialGeneracionService.wasCalled());
    }

    @Test
    void registroTitular_usuarioExistente_sinPassword_creaMemberEnClub() {
        MemberPublicRegistroCommand command = publicCommand("existente@test.com", null);

        User existing = new User();
        existing.setId(60L);
        existing.setUuid("existing-user-uuid");
        existing.setEmail("existente@test.com");
        existing.setDni("30111222");
        existing.setUserCompanyRoles(new ArrayList<>());

        User existingWithDetails = new User();
        existingWithDetails.setId(60L);
        existingWithDetails.setUuid("existing-user-uuid");
        existingWithDetails.setEmail("existente@test.com");
        existingWithDetails.setDni("30111222");
        existingWithDetails.setUserCompanyRoles(new ArrayList<>());

        command.setDni("30111222");

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "existente@test.com")).thenReturn(existing);
        when(userRepository.findFirstByUuidWithDetails("existing-user-uuid")).thenReturn(existingWithDetails);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company)).thenReturn(socioTitularRole);
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(existingWithDetails);
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(60L), anyCollection()))
                .thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(2L);
            m.setUuid("member-2");
            assertEquals(MemberStatusConstants.STATUS_PENDING_APPROVAL, m.getStatus());
            return m;
        });
        when(memberRepository.findByIdWithTitular(2L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(2L);
            m.setUuid("member-2");
            m.setCompany(company);
            m.setUser(existingWithDetails);
            m.setEstado(MemberEstadoEnum.ACTIVO);
            return Optional.of(m);
        });

        MemberDTO result = publicService.registroTitular("club-uuid", command);

        assertEquals("member-2", result.getMemberUuid());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registroTitular_usuarioYaEsSocioDelClub_devuelve409() {
        MemberPublicRegistroCommand command = publicCommand("socio@test.com", null);

        User existing = new User();
        existing.setId(70L);
        existing.setUuid("socio-user-uuid");
        existing.setDni("30111222");
        existing.setUserCompanyRoles(new ArrayList<>());

        command.setDni("30111222");

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "socio@test.com")).thenReturn(existing);
        when(userRepository.findFirstByUuidWithDetails("socio-user-uuid")).thenReturn(existing);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company)).thenReturn(socioTitularRole);
        when(memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(eq(10L), eq(70L), anyCollection()))
                .thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> publicService.registroTitular("club-uuid", command));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void registroTitular_companyInvalida_devuelve400() {
        when(companyRepository.findFirstByUuidWithStatusIsActive("invalid-uuid")).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> publicService.registroTitular("invalid-uuid", publicCommand("a@test.com", "pwd")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void registroTitular_sinCompanyUuid_devuelve400() {
        StubAuthService passThroughAuth = new StubAuthService() {
            @Override
            public String resolveCompanyUuid(String companyUuid) {
                return companyUuid;
            }
        };
        ReflectionTestUtils.setField(publicService, "authService", passThroughAuth);

        CustomException ex = assertThrows(CustomException.class,
                () -> publicService.registroTitular(null, publicCommand("a@test.com", "pwd")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void registroTitular_usuarioNuevo_sinPassword_devuelve400() {
        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);

        CustomException ex = assertThrows(CustomException.class,
                () -> publicService.registroTitular("club-uuid", publicCommand("nuevo@test.com", null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // El alta publica no pasa por AccesoBloqueadoInterceptor (no hay JWT del que resolver la
    // company), asi que el corte lo hace el propio service: club bloqueado, 403 y no se crea nada.
    @Test
    void registroTitular_clubBloqueado_devuelve403YNoCreaNada() {
        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyAccesoService.estaBloqueada(company)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> publicService.registroTitular("club-uuid", publicCommand("nuevo@test.com", "secret123")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        // Mismo formato que el 403 del interceptor, para que el front no distinga dos respuestas
        assertEquals("FACTURA_VENCIDA", ex.getErrores().get(0).getValue());
        verify(memberRepository, never()).save(any(Member.class));
        verify(userRepository, never()).save(any(User.class));
    }

    private MemberPublicRegistroCommand publicCommand(String email, String password) {
        MemberPublicRegistroCommand command = new MemberPublicRegistroCommand();
        command.setNombre("Juan");
        command.setApellido("Pérez");
        command.setEmail(email);
        command.setPassword(password);
        command.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return command;
    }
}
