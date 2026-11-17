package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberRegistroRechazoCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.support.RecordingCuotaSocialGeneracionService;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberRegistroAprobacionServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    private RecordingCuotaSocialGeneracionService cuotaSocialGeneracionService;
    private MemberRegistroAprobacionService aprobacionService;
    private StubAuthService stubAuthService;
    private Company company;
    private Member pendingMember;
    private User pendingUser;

    @BeforeEach
    void setUp() {
        MemberMapper memberMapper = new MemberMapper();
        ReflectionTestUtils.setField(memberMapper, "memberRepository", memberRepository);
        cuotaSocialGeneracionService = new RecordingCuotaSocialGeneracionService();

        CrmPlanLimiteResolver limiteResolver = new CrmPlanLimiteResolver();
        ReflectionTestUtils.setField(limiteResolver, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        MemberCupoService memberCupoService = new MemberCupoService();
        ReflectionTestUtils.setField(memberCupoService, "crmPlanLimiteResolver", limiteResolver);
        ReflectionTestUtils.setField(memberCupoService, "memberRepository", memberRepository);

        MemberNumeroSocioService numeroSocioService = new MemberNumeroSocioService();
        ReflectionTestUtils.setField(numeroSocioService, "memberRepository", memberRepository);

        aprobacionService = new MemberRegistroAprobacionService();
        stubAuthService = new StubAuthService();
        ReflectionTestUtils.setField(aprobacionService, "authService", stubAuthService);
        ReflectionTestUtils.setField(aprobacionService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(aprobacionService, "userRepository", userRepository);
        ReflectionTestUtils.setField(aprobacionService, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(aprobacionService, "cuotaSocialGeneracionService", cuotaSocialGeneracionService);
        ReflectionTestUtils.setField(aprobacionService, "memberCupoService", memberCupoService);
        ReflectionTestUtils.setField(aprobacionService, "numeroSocioService", numeroSocioService);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        company.setCuotaSocialHabilitada(true);
        stubAuthService.setCompany(company);

        // Cupo con lugar de sobra por defecto: lenient() porque rechazarRegistro no lo consulta
        // y los tests de cupo pisan este stub con su propia configuracion.
        lenient().when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(999)));

        pendingUser = new User();
        pendingUser.setId(50L);
        pendingUser.setUuid("user-uuid");
        pendingUser.setStatus(MemberStatusConstants.STATUS_PENDING_APPROVAL);
        pendingUser.setNombre("Juan");
        pendingUser.setApellido("Pérez");
        pendingUser.setEmail("juan@test.com");
        pendingUser.setUsername("juan@test.com");
        pendingUser.setDni("30111222");

        pendingMember = new Member();
        pendingMember.setId(1L);
        pendingMember.setUuid("member-uuid");
        pendingMember.setCompany(company);
        pendingMember.setUser(pendingUser);
        pendingMember.setStatus(MemberStatusConstants.STATUS_PENDING_APPROVAL);
        pendingMember.setEstado(MemberEstadoEnum.ACTIVO);
        pendingMember.setFechaAlta(LocalDate.now());
        pendingMember.setNumeroSocio(null);
    }

    @Test
    void aprobarRegistro_activaMemberYUserNuevo() {
        when(memberRepository.findByUuidAndCompanyIdAndStatus(
                "member-uuid", 10L, MemberStatusConstants.STATUS_PENDING_APPROVAL))
                .thenReturn(Optional.of(pendingMember));
        when(memberRepository.findMaxNumeroSocioByCompanyId(10L)).thenReturn(null);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdWithTitular(1L)).thenReturn(Optional.of(pendingMember));

        MemberDTO result = aprobacionService.aprobarRegistro("member-uuid", null);

        assertEquals("member-uuid", result.getMemberUuid());
        assertEquals("1", pendingMember.getNumeroSocio());
        assertEquals(MemberStatusConstants.STATUS_ACTIVE, pendingMember.getStatus());
        assertEquals(MemberStatusConstants.STATUS_ACTIVE, pendingUser.getStatus());
        assertEquals(1L, cuotaSocialGeneracionService.getLastMemberId());
        assertEquals(pendingMember.getFechaAlta(), cuotaSocialGeneracionService.getLastFechaAlta());
        verify(userRepository).save(pendingUser);
    }

    @Test
    void aprobarRegistro_usuarioExistente_noModificaUserActivo() {
        pendingUser.setStatus(MemberStatusConstants.STATUS_ACTIVE);

        when(memberRepository.findByUuidAndCompanyIdAndStatus(
                "member-uuid", 10L, MemberStatusConstants.STATUS_PENDING_APPROVAL))
                .thenReturn(Optional.of(pendingMember));
        when(memberRepository.findMaxNumeroSocioByCompanyId(10L)).thenReturn(null);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdWithTitular(1L)).thenReturn(Optional.of(pendingMember));

        aprobacionService.aprobarRegistro("member-uuid", null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void aprobarRegistro_noPendiente_devuelve404() {
        when(memberRepository.findByUuidAndCompanyIdAndStatus(
                "missing", 10L, MemberStatusConstants.STATUS_PENDING_APPROVAL))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> aprobacionService.aprobarRegistro("missing", null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // Dado un club sin cupo disponible en su plan del CRM, cuando aprobarRegistro, entonces
    // corta antes de tocar el Member pendiente (no lo activa).
    @Test
    void aprobarRegistro_sinCupoDisponible_noActivaMember() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.of(planConCupo(1)));
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE))
                .thenReturn(1L);

        CustomException ex = assertThrows(CustomException.class,
                () -> aprobacionService.aprobarRegistro("member-uuid", null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
        verify(memberRepository, never()).findByUuidAndCompanyIdAndStatus(any(), any(), anyInt());
    }

    // Dado un club sin CompanySuscripcionCrm (plan sin sincronizar), cuando aprobarRegistro,
    // entonces corta: el limite indeterminado bloquea, no deja pasar.
    @Test
    void aprobarRegistro_sinPlanConfigurado_noActivaMember() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, 0))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> aprobacionService.aprobarRegistro("member-uuid", null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(memberRepository, never()).save(any(Member.class));
    }

    private CompanySuscripcionCrm planConCupo(int unidades) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar(null, null, null, null, null, null,
                "[{\"codigo\":\"bsports-socios\",\"nombre\":\"Socios\",\"unidades\":" + unidades + "}]");
        return CompanySuscripcionCrm.crear(company, plan, null, null, null, null);
    }

    @Test
    void rechazarRegistro_usuarioNuevoSinOtrosMembers_eliminaUser() {
        when(memberRepository.findByUuidAndCompanyIdAndStatus(
                "member-uuid", 10L, MemberStatusConstants.STATUS_PENDING_APPROVAL))
                .thenReturn(Optional.of(pendingMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.countByUser_IdAndStatus(50L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberRegistroRechazoCommand command = new MemberRegistroRechazoCommand();
        command.setMotivo("Datos incorrectos");

        aprobacionService.rechazarRegistro("member-uuid", command);

        assertEquals(MemberStatusConstants.STATUS_DELETED, pendingMember.getStatus());
        assertEquals(MemberStatusConstants.STATUS_DELETED, pendingUser.getStatus());
        assertEquals("deleted-50-juan@test.com", pendingUser.getEmail());
        assertEquals("deleted-50-juan@test.com", pendingUser.getUsername());
        assertEquals("deleted-50-30111222", pendingUser.getDni());
        assertEquals("Usuario", pendingUser.getNombre());
        assertEquals("Eliminado", pendingUser.getApellido());
        verify(userRepository).save(pendingUser);
    }

    @Test
    void rechazarRegistro_usuarioExistenteActivo_noEliminaUser() {
        pendingUser.setStatus(MemberStatusConstants.STATUS_ACTIVE);

        when(memberRepository.findByUuidAndCompanyIdAndStatus(
                "member-uuid", 10L, MemberStatusConstants.STATUS_PENDING_APPROVAL))
                .thenReturn(Optional.of(pendingMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        aprobacionService.rechazarRegistro("member-uuid", null);

        assertEquals(MemberStatusConstants.STATUS_DELETED, pendingMember.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }
}
