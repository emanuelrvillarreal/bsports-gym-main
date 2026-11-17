package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoActividadAutorizacionServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ProfesorAuthService profesorAuthService;

    @Mock
    private MemberAuthService memberAuthService;

    private StubAuthService stubAuthService;
    private EventoActividadAutorizacionService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new EventoActividadAutorizacionService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(service, "profesorAuthService", profesorAuthService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
    }

    // CA-01/CA-08: actividad desactivada rechaza alta
    @Test
    void requireActividadActivaParaAlta_rechazaActividadInactiva() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireActividadActivaParaAlta("actividad-uuid", 1L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // CA-01: profesor no asignado rechazado en portal
    @Test
    void requireAlcance_rechazaProfesorNoAsignadoEnPortal() {
        User user = new User();
        user.setId(7L);
        stubAuthService.setUser(user);

        Actividad actividad = new Actividad();
        actividad.setId(20L);

        Profesor profesor = new Profesor();
        profesor.setId(3L);

        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(20L, 3L, 0)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireAlcanceStaffOProfesorAsignado(actividad, 1L, true));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // CA-01: profesor rechazado en backoffice
    @Test
    void requireAlcance_rechazaProfesorEnBackoffice() {
        User user = new User();
        user.setId(7L);
        stubAuthService.setUser(user);

        Actividad actividad = new Actividad();
        actividad.setId(20L);

        when(profesorRepository.findByUserIdAndCompanyIdWithUser(7L, 1L))
                .thenReturn(Optional.of(new Profesor()));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireAlcanceStaffOProfesorAsignado(actividad, 1L, false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // CA-04/CA-07: staff sin restricción adicional cuando no es profesor en backoffice
    @Test
    void requireAlcance_permiteStaffEnBackoffice() {
        User user = new User();
        user.setId(1L);
        stubAuthService.setUser(user);

        Actividad actividad = new Actividad();
        actividad.setId(20L);

        when(profesorRepository.findByUserIdAndCompanyIdWithUser(1L, 1L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requireAlcanceStaffOProfesorAsignado(actividad, 1L, false));
    }
}
