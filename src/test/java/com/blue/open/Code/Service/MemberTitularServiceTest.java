package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoResolver;
import com.blue.open.Code.Service.support.RecordingCuotaSocialGeneracionService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTitularServiceTest {

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
    @Mock
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;
    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    private MemberTitularService service;
    private StubAuthService stubAuthService;
    private Company company;
    private RecordingCuotaSocialGeneracionService cuotaSocialGeneracionService;

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

        CuotaSocialRangoResolver cuotaSocialRangoResolver = new CuotaSocialRangoResolver();
        ReflectionTestUtils.setField(cuotaSocialRangoResolver, "cuotaSocialRangoRepository", cuotaSocialRangoRepository);

        CrmPlanLimiteResolver limiteResolver = new CrmPlanLimiteResolver();
        ReflectionTestUtils.setField(limiteResolver, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        MemberCupoService memberCupoService = new MemberCupoService();
        ReflectionTestUtils.setField(memberCupoService, "crmPlanLimiteResolver", limiteResolver);
        ReflectionTestUtils.setField(memberCupoService, "memberRepository", memberRepository);

        stubAuthService = new StubAuthService();
        cuotaSocialGeneracionService = new RecordingCuotaSocialGeneracionService();

        service = new MemberTitularService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "cuotaSocialGeneracionService", cuotaSocialGeneracionService);
        ReflectionTestUtils.setField(service, "userProvisioningService", provisioningService);
        ReflectionTestUtils.setField(service, "numeroSocioService", numeroSocioService);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "cuotaSocialRangoResolver", cuotaSocialRangoResolver);
        ReflectionTestUtils.setField(service, "memberCupoService", memberCupoService);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        company.setCuotaSocialHabilitada(true);
        stubAuthService.setCompany(company);

        // Cupo con lugar de sobra por defecto: los tests existentes no tratan sobre el limite de
        // socios, y altaTitular ahora lo valida antes que nada. lenient() porque los tests de
        // cupo pisan este stub con su propia configuracion.
        lenient().when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(999)));
    }

    @Test
    void altaTitular_autogeneraNumero_yGeneraCuotaSocial() {
        MemberTitularAltaCommand command = altaCommand("socio@test.com");

        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            u.setUuid("user-uuid");
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findMaxNumeroSocioByCompanyId(10L)).thenReturn(null);
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(10L), any()))
                .thenReturn(Optional.of(new CuotaSocialRango()));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(1L);
            m.setUuid("member-uuid");
            return m;
        });
        when(memberRepository.findByIdWithTitular(1L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(1L);
            m.setUuid("member-uuid");
            m.setCompany(company);
            m.setUser(userFromCommand(command));
            m.setEstado(MemberEstadoEnum.ACTIVO);
            m.setNumeroSocio("1");
            m.setFechaAlta(LocalDate.of(2026, 6, 1));
            return Optional.of(m);
        });

        MemberDTO result = service.altaTitular(command);

        assertEquals("member-uuid", result.getMemberUuid());
        assertEquals("1", result.getNumeroSocio());
        assertEquals(1L, cuotaSocialGeneracionService.getLastMemberId());
        assertEquals(LocalDate.of(2026, 6, 1), cuotaSocialGeneracionService.getLastFechaAlta());
    }

    @Test
    void altaTitular_autogeneraNumero_delegaGeneracionCuotaSocial() {
        MemberTitularAltaCommand command = altaCommand("otro@test.com");

        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(51L);
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findMaxNumeroSocioByCompanyId(10L)).thenReturn(null);
        when(cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(eq(10L), any()))
                .thenReturn(Optional.of(new CuotaSocialRango()));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(2L);
            m.setUuid("m2");
            m.setCompany(company);
            m.setUser(new User());
            m.setEstado(MemberEstadoEnum.ACTIVO);
            return m;
        });
        when(memberRepository.findByIdWithTitular(2L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(2L);
            m.setUuid("m2");
            m.setCompany(company);
            m.setNumeroSocio("1");
            m.setEstado(MemberEstadoEnum.ACTIVO);
            return Optional.of(m);
        });

        service.altaTitular(command);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertEquals("1", captor.getValue().getNumeroSocio());
        assertEquals(2L, cuotaSocialGeneracionService.getLastMemberId());
        assertEquals(LocalDate.of(2026, 6, 1), cuotaSocialGeneracionService.getLastFechaAlta());
    }

    @Test
    void altaTitular_rechazaEmailDuplicado() {
        MemberTitularAltaCommand command = altaCommand("dup@test.com");
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        User existing = new User();
        existing.setUuid("other-uuid");

        when(userRepository.findFirstByUsernameIgnoringCase("dup@test.com")).thenReturn(existing);

        CustomException ex = assertThrows(CustomException.class, () -> service.altaTitular(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // Dado un club sin cupo disponible en su plan del CRM, cuando altaTitular, entonces corta
    // antes de crear nada (ni User ni Member) — "antes que nada".
    @Test
    void altaTitular_sinCupoDisponible_noCreaNada() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(1)));
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE))
                .thenReturn(1L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.altaTitular(altaCommand("sincupo@test.com")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
        verify(userRepository, never()).save(any(User.class));
    }

    // Dado un club sin CompanySuscripcionCrm (plan sin sincronizar), cuando altaTitular,
    // entonces corta: el limite indeterminado bloquea, no deja pasar.
    @Test
    void altaTitular_sinPlanConfigurado_noCreaNada() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.altaTitular(altaCommand("sinplan@test.com")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
    }

    private CompanySuscripcionCrm planConCupo(int unidades) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar(null, null, null, null, null, null,
                "[{\"codigo\":\"bsports-socios\",\"nombre\":\"Socios\",\"unidades\":" + unidades + "}]");
        return CompanySuscripcionCrm.crear(company, plan, null, null, null, null);
    }

    private MemberTitularAltaCommand altaCommand(String email) {
        MemberTitularAltaCommand command = new MemberTitularAltaCommand();
        command.setNombre("Juan");
        command.setApellido("Pérez");
        command.setEmail(email);
        command.setPassword("secret123");
        command.setFechaAlta(LocalDate.of(2026, 6, 1));
        command.setFechaNacimiento(LocalDate.of(1990, 3, 15));
        return command;
    }

    private User userFromCommand(MemberTitularAltaCommand command) {
        User user = new User();
        user.setNombre(command.getNombre());
        user.setApellido(command.getApellido());
        user.setEmail(command.getEmail());
        return user;
    }
}
