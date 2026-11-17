package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberFamiliarAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
class MemberFamiliarServiceTest {

    @Mock
    private MemberAuthService memberAuthService;
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

    private MemberFamiliarService service;
    private StubAuthService stubAuthService;
    private Company company;

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

        CuotaSocialRangoResolver cuotaSocialRangoResolver = new CuotaSocialRangoResolver();
        ReflectionTestUtils.setField(cuotaSocialRangoResolver, "cuotaSocialRangoRepository", cuotaSocialRangoRepository);

        CrmPlanLimiteResolver limiteResolver = new CrmPlanLimiteResolver();
        ReflectionTestUtils.setField(limiteResolver, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        MemberCupoService memberCupoService = new MemberCupoService();
        ReflectionTestUtils.setField(memberCupoService, "crmPlanLimiteResolver", limiteResolver);
        ReflectionTestUtils.setField(memberCupoService, "memberRepository", memberRepository);

        MemberNumeroSocioService numeroSocioService = new MemberNumeroSocioService();
        ReflectionTestUtils.setField(numeroSocioService, "memberRepository", memberRepository);

        stubAuthService = new StubAuthService();
        service = new MemberFamiliarService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "userProvisioningService", provisioningService);
        ReflectionTestUtils.setField(service, "numeroSocioService", numeroSocioService);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "cuotaSocialRangoResolver", cuotaSocialRangoResolver);
        ReflectionTestUtils.setField(service, "cuotaSocialGeneracionService", new RecordingCuotaSocialGeneracionService());
        ReflectionTestUtils.setField(service, "memberCupoService", memberCupoService);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        stubAuthService.setCompany(company);

        // Cupo con lugar de sobra por defecto: lenient() porque los tests de cupo pisan este
        // stub con su propia configuracion.
        lenient().when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(999)));
    }

    @Test
    void altaFamiliar_ok() {
        Member titular = titular(1L, MemberEstadoEnum.ACTIVO);
        MemberFamiliarAltaCommand command = familiarCommand();

        when(memberAuthService.resolveTitularByUuid("titular-uuid", 10L)).thenReturn(titular);
        when(userRepository.findFirstByEmailAndStatusNot(eq("fam@test.com"), eq(-1))).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.FAMILIAR.name(), company)).thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(60L);
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findMaxNumeroSocioByCompanyId(10L)).thenReturn(null);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(5L);
            m.setUuid("fam-uuid");
            m.setTitular(titular);
            m.setParentesco(ParentescoEnum.HIJO);
            m.setCompany(company);
            m.setUser(new User());
            return m;
        });
        when(memberRepository.findByIdWithTitular(5L)).thenAnswer(inv -> {
            Member m = new Member();
            m.setId(5L);
            m.setUuid("fam-uuid");
            m.setTitular(titular);
            m.setParentesco(ParentescoEnum.HIJO);
            m.setCompany(company);
            m.setUser(new User());
            return Optional.of(m);
        });

        var result = service.altaFamiliar(command);

        assertEquals("fam-uuid", result.getMemberUuid());
        assertEquals(ParentescoEnum.HIJO, result.getParentesco());
    }

    @Test
    void altaFamiliar_rechazaSinParentesco() {
        MemberFamiliarAltaCommand command = familiarCommand();
        command.setParentesco(null);

        CustomException ex = assertThrows(CustomException.class, () -> service.altaFamiliar(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void altaFamiliar_rechazaTitularInhabilitado() {
        when(memberAuthService.resolveTitularByUuid("titular-uuid", 10L))
                .thenReturn(titular(1L, MemberEstadoEnum.INHABILITADO));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.altaFamiliar(familiarCommand()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // Dado un club sin cupo disponible en su plan del CRM, cuando altaFamiliar, entonces corta
    // antes de resolver el titular ni crear nada.
    @Test
    void altaFamiliar_sinCupoDisponible_noCreaNada() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(1)));
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE))
                .thenReturn(1L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.altaFamiliar(familiarCommand()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
        verify(memberAuthService, never()).resolveTitularByUuid(any(), any());
    }

    // Dado un club sin CompanySuscripcionCrm (plan sin sincronizar), cuando altaFamiliar,
    // entonces corta: el limite indeterminado bloquea, no deja pasar.
    @Test
    void altaFamiliar_sinPlanConfigurado_noCreaNada() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.altaFamiliar(familiarCommand()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
    }

    private CompanySuscripcionCrm planConCupo(int unidades) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar(null, null, null, null, null, null,
                "[{\"codigo\":\"bsports-socios\",\"nombre\":\"Socios\",\"unidades\":" + unidades + "}]");
        return CompanySuscripcionCrm.crear(company, plan, null, null, null, null);
    }

    private Member titular(Long id, MemberEstadoEnum estado) {
        Member m = new Member();
        m.setId(id);
        m.setEstado(estado);
        m.setCompany(company);
        return m;
    }

    private MemberFamiliarAltaCommand familiarCommand() {
        MemberFamiliarAltaCommand command = new MemberFamiliarAltaCommand();
        command.setTitularMemberUuid("titular-uuid");
        command.setParentesco(ParentescoEnum.HIJO);
        command.setNombre("Ana");
        command.setApellido("Pérez");
        command.setEmail("fam@test.com");
        command.setPassword("secret123");
        return command;
    }
}
