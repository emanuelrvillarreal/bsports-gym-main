package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AccesoClubTipoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Event.AccesoClubRegistroEvent;
import com.blue.open.Code.Mapper.AccesoClubMapper;
import com.blue.open.Code.Repository.AccesoClubRepository;
import com.blue.open.Code.Repository.ClubPresenciaRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoClubRegistroServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long USER_ID = 5L;
    private static final Long MEMBER_ID = 99L;
    private static final String USER_UUID = "user-uuid-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccesoClubRepository accesoClubRepository;

    @Mock
    private ClubPresenciaRepository clubPresenciaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final StubAuthService stubAuthService = new StubAuthService();

    private AccesoClubRegistroService service;

    @Mock
    private com.blue.open.Code.Service.Membresias.MembresiaAccesoService membresiaAccesoService;

    @Mock
    private com.blue.open.Code.Service.Membresias.MembresiaSoporte membresiaSoporte;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        stubAuthService.setCompany(company);

        service = new AccesoClubRegistroService(
                stubAuthService,
                userRepository,
                memberRepository,
                accesoClubRepository,
                clubPresenciaRepository,
                new AccesoClubMapper(),
                eventPublisher,
                membresiaAccesoService,
                membresiaSoporte);
        // Sin pases cargados el ingreso sigue como antes.
        lenient().when(membresiaAccesoService.evaluar(any(), any(), any(), any()))
                .thenReturn(new com.blue.open.Code.Entity.DTO.MembresiaDTO.EvaluacionAcceso(true, null, null));

        lenient().when(userRepository.findFirstByUuidWithDetails(USER_UUID)).thenReturn(buildUser());
        lenient().when(memberRepository.findByUserIdAndCompanyIdWithDetails(USER_ID, COMPANY_ID))
                .thenReturn(Optional.empty());
        lenient().when(accesoClubRepository.save(any(AccesoClub.class))).thenAnswer(invocation -> {
            AccesoClub acceso = invocation.getArgument(0);
            acceso.setUuid("acceso-" + acceso.getTipo().name());
            return acceso;
        });
    }

    @Test
    void registrar_sinPresenciaPrevio_registraIngresoEInsertaPresenciaPresente() {
        when(clubPresenciaRepository.findByCompanyIdAndUserIdWithUser(COMPANY_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(clubPresenciaRepository.findByCompanyIdAndUserId(COMPANY_ID, USER_ID))
                .thenReturn(Optional.empty());

        AccesoClubRegistroDTO result = service.registrar(USER_UUID);

        ArgumentCaptor<AccesoClub> accesoCaptor = ArgumentCaptor.forClass(AccesoClub.class);
        verify(accesoClubRepository).save(accesoCaptor.capture());
        assertEquals(AccesoClubTipoEnum.INGRESO, accesoCaptor.getValue().getTipo());
        assertEquals(COMPANY_ID, accesoCaptor.getValue().getCompany().getId());
        assertEquals(USER_ID, accesoCaptor.getValue().getUser().getId());
        assertNull(accesoCaptor.getValue().getMemberEstado());

        verify(clubPresenciaRepository).insertPresenciaByCompanyIdAndUserId(
                anyString(),
                eq(COMPANY_ID),
                eq(USER_ID),
                eq(true),
                any(LocalDateTime.class));
        verify(clubPresenciaRepository, never()).updatePresenciaByCompanyIdAndUserId(
                eq(COMPANY_ID), eq(USER_ID), any(Boolean.class), any(LocalDateTime.class));

        assertEquals(AccesoClubTipoEnum.INGRESO, result.getTipo());
        assertTrue(result.isPresente());
        assertEquals(USER_UUID, result.getUserUuid());
        assertNull(result.getAdvertencia());

        ArgumentCaptor<AccesoClubRegistroEvent> eventCaptor = ArgumentCaptor.forClass(AccesoClubRegistroEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(COMPANY_ID, eventCaptor.getValue().getCompanyId());
        assertEquals(result, eventCaptor.getValue().getRegistro());
    }

    @Test
    void registrar_usuarioPresente_registraEgresoYActualizaPresenciaAusente() {
        ClubPresencia presencia = buildPresencia(true);
        when(clubPresenciaRepository.findByCompanyIdAndUserIdWithUser(COMPANY_ID, USER_ID))
                .thenReturn(Optional.of(presencia));
        when(clubPresenciaRepository.findByCompanyIdAndUserId(COMPANY_ID, USER_ID))
                .thenReturn(Optional.of(presencia));

        AccesoClubRegistroDTO result = service.registrar(USER_UUID);

        ArgumentCaptor<AccesoClub> accesoCaptor = ArgumentCaptor.forClass(AccesoClub.class);
        verify(accesoClubRepository).save(accesoCaptor.capture());
        assertEquals(AccesoClubTipoEnum.EGRESO, accesoCaptor.getValue().getTipo());

        verify(clubPresenciaRepository).updatePresenciaByCompanyIdAndUserId(
                eq(COMPANY_ID),
                eq(USER_ID),
                eq(false),
                any(LocalDateTime.class));
        verify(clubPresenciaRepository, never()).insertPresenciaByCompanyIdAndUserId(
                anyString(), eq(COMPANY_ID), eq(USER_ID), any(Boolean.class), any(LocalDateTime.class));

        assertEquals(AccesoClubTipoEnum.EGRESO, result.getTipo());
        assertEquals(false, result.isPresente());
    }

    @Test
    void registrar_usuarioAusenteConFila_registraIngresoYActualizaPresenciaPresente() {
        ClubPresencia presencia = buildPresencia(false);
        when(clubPresenciaRepository.findByCompanyIdAndUserIdWithUser(COMPANY_ID, USER_ID))
                .thenReturn(Optional.of(presencia));
        when(clubPresenciaRepository.findByCompanyIdAndUserId(COMPANY_ID, USER_ID))
                .thenReturn(Optional.of(presencia));

        AccesoClubRegistroDTO result = service.registrar(USER_UUID);

        ArgumentCaptor<AccesoClub> accesoCaptor = ArgumentCaptor.forClass(AccesoClub.class);
        verify(accesoClubRepository).save(accesoCaptor.capture());
        assertEquals(AccesoClubTipoEnum.INGRESO, accesoCaptor.getValue().getTipo());

        verify(clubPresenciaRepository).updatePresenciaByCompanyIdAndUserId(
                eq(COMPANY_ID),
                eq(USER_ID),
                eq(true),
                any(LocalDateTime.class));

        assertEquals(AccesoClubTipoEnum.INGRESO, result.getTipo());
        assertTrue(result.isPresente());
    }

    @Test
    void registrar_dosPasadasAlternanTipoYSincronizanPresencia() {
        AtomicBoolean presente = new AtomicBoolean(false);
        ClubPresencia presencia = new ClubPresencia();

        when(clubPresenciaRepository.findByCompanyIdAndUserIdWithUser(COMPANY_ID, USER_ID))
                .thenAnswer(inv -> {
                    if (!presente.get()) {
                        return Optional.empty();
                    }
                    presencia.setPresente(true);
                    return Optional.of(presencia);
                });
        when(clubPresenciaRepository.findByCompanyIdAndUserId(COMPANY_ID, USER_ID))
                .thenAnswer(inv -> presente.get() ? Optional.of(presencia) : Optional.empty());
        when(clubPresenciaRepository.insertPresenciaByCompanyIdAndUserId(
                anyString(), eq(COMPANY_ID), eq(USER_ID), eq(true), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    presente.set(true);
                    presencia.setPresente(true);
                    return 1;
                });
        when(clubPresenciaRepository.updatePresenciaByCompanyIdAndUserId(
                eq(COMPANY_ID), eq(USER_ID), eq(false), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    presente.set(false);
                    presencia.setPresente(false);
                    return 1;
                });

        AccesoClubRegistroDTO ingreso = service.registrar(USER_UUID);
        assertEquals(AccesoClubTipoEnum.INGRESO, ingreso.getTipo());
        assertTrue(ingreso.isPresente());

        AccesoClubRegistroDTO egreso = service.registrar(USER_UUID);
        assertEquals(AccesoClubTipoEnum.EGRESO, egreso.getTipo());
        assertEquals(false, egreso.isPresente());
    }

    @Test
    void registrar_socioActivo_persisteMemberEstadoSinAdvertencia() {
        stubMember(MemberEstadoEnum.ACTIVO);
        stubIngresoSinPresencia();

        AccesoClubRegistroDTO result = service.registrar(USER_UUID);

        ArgumentCaptor<AccesoClub> accesoCaptor = ArgumentCaptor.forClass(AccesoClub.class);
        verify(accesoClubRepository).save(accesoCaptor.capture());
        assertEquals(MemberEstadoEnum.ACTIVO, accesoCaptor.getValue().getMemberEstado());
        assertEquals(MEMBER_ID, accesoCaptor.getValue().getMember().getId());
        assertEquals(MemberEstadoEnum.ACTIVO, result.getMemberEstado());
        assertNull(result.getAdvertencia());
    }

    @Test
    void registrar_socioMoroso_registraConAdvertencia() {
        stubMember(MemberEstadoEnum.MOROSO);
        stubIngresoSinPresencia();

        AccesoClubRegistroDTO result = service.registrar(USER_UUID);

        ArgumentCaptor<AccesoClub> accesoCaptor = ArgumentCaptor.forClass(AccesoClub.class);
        verify(accesoClubRepository).save(accesoCaptor.capture());
        assertEquals(MemberEstadoEnum.MOROSO, accesoCaptor.getValue().getMemberEstado());
        assertEquals(MemberEstadoEnum.MOROSO, result.getMemberEstado());
        assertEquals("Socio moroso. El acceso se registró con advertencia.", result.getAdvertencia());
    }

    @ParameterizedTest
    @EnumSource(value = MemberEstadoEnum.class, names = {"INHABILITADO", "SUSPENDIDO", "INACTIVO"})
    void registrar_socioNoHabilitado_rechazaIngresoYEgreso(MemberEstadoEnum estado) {
        stubMember(estado);

        CustomException ex = assertThrows(CustomException.class, () -> service.registrar(USER_UUID));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(accesoClubRepository, never()).save(any());
        verify(clubPresenciaRepository, never()).insertPresenciaByCompanyIdAndUserId(
                anyString(), eq(COMPANY_ID), eq(USER_ID), any(Boolean.class), any(LocalDateTime.class));
        verify(clubPresenciaRepository, never()).updatePresenciaByCompanyIdAndUserId(
                eq(COMPANY_ID), eq(USER_ID), any(Boolean.class), any(LocalDateTime.class));
    }

    @Test
    void registrar_socioSuspendidoPresente_rechazaEgreso() {
        stubMember(MemberEstadoEnum.SUSPENDIDO);

        CustomException ex = assertThrows(CustomException.class, () -> service.registrar(USER_UUID));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(accesoClubRepository, never()).save(any());
    }

    private void stubIngresoSinPresencia() {
        when(clubPresenciaRepository.findByCompanyIdAndUserIdWithUser(COMPANY_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(clubPresenciaRepository.findByCompanyIdAndUserId(COMPANY_ID, USER_ID))
                .thenReturn(Optional.empty());
    }

    private void stubMember(MemberEstadoEnum estado) {
        Member member = buildMember(estado);
        when(memberRepository.findByUserIdAndCompanyIdWithDetails(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(member));
    }

    private Member buildMember(MemberEstadoEnum estado) {
        Member member = new Member();
        member.setId(MEMBER_ID);
        member.setEstado(estado);
        member.setFechaAlta(LocalDate.now());
        member.setUser(buildUser());
        return member;
    }

    private User buildUser() {
        Company company = stubAuthService.getCurrentCompanyOrThrow();
        UserCompanyRole userCompanyRole = new UserCompanyRole();
        userCompanyRole.setCompany(company);

        User user = new User();
        user.setId(USER_ID);
        user.setUuid(USER_UUID);
        user.setStatus(0);
        user.setNombre("Juan");
        user.setApellido("Pérez");
        user.setDni("30123456");
        user.setUserCompanyRoles(List.of(userCompanyRole));
        return user;
    }

    private ClubPresencia buildPresencia(boolean presente) {
        ClubPresencia presencia = new ClubPresencia();
        presencia.setPresente(presente);
        presencia.setUltimoAccesoAt(LocalDateTime.now().minusHours(1));
        presencia.setUser(buildUser());
        return presencia;
    }

    private static class StubAuthService extends AuthService {

        private Company company = new Company();

        void setCompany(Company company) {
            this.company = company;
        }

        @Override
        public User valid() {
            return new User();
        }

        @Override
        public User valid(List<Enum<?>> required) {
            return valid();
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            return company;
        }
    }
}
