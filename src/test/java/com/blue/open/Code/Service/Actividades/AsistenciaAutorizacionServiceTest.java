package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.SlotRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaAutorizacionServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ProfesorAuthService profesorAuthService;

    private StubAuthService stubAuthService;
    private AsistenciaAutorizacionService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new AsistenciaAutorizacionService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(service, "profesorAuthService", profesorAuthService);
    }

    // CA-01: profesor activo asignado puede operar
    @Test
    void requireSlotOperable_permiteProfesorAsignadoActivo() {
        Slot slot = slotActivo();
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        configurarProfesorAsignado(1L, 10L, 5L);

        Slot result = service.requireSlotOperable("slot-uuid", 1L, true);

        assertNotNull(result);
        assertEquals("slot-uuid", result.getUuid());
    }

    // CA-01: profesor inactivo rechazado
    @Test
    void requireSlotOperable_rechazaProfesorInactivo() {
        Slot slot = slotActivo();
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));

        Profesor profesor = new Profesor();
        profesor.setId(5L);
        profesor.setEstado(ProfesorEstadoEnum.INACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor))
                .thenThrow(new CustomException(HttpStatus.FORBIDDEN, "Acceso al portal no disponible", "test"));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireSlotOperable("slot-uuid", 1L, true));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // CA-01: profesor no asignado rechazado
    @Test
    void requireSlotOperable_rechazaProfesorNoAsignado() {
        Slot slot = slotActivo();
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        configurarProfesorAsignado(1L, 10L, 5L);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 5L, 0)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireSlotOperable("slot-uuid", 1L, true));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // CA-01: slot cancelado rechazado
    @Test
    void requireSlotOperable_rechazaSlotCancelado() {
        Slot slot = slotActivo();
        slot.setEstado(SlotEstadoEnum.CANCELADO);
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireSlotOperable("slot-uuid", 1L, true));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // CA-01: actividad inactiva rechazada
    @Test
    void requireSlotOperable_rechazaActividadInactiva() {
        Slot slot = slotActivo();
        slot.getActividad().setStatus(-1);
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireSlotOperable("slot-uuid", 1L, true));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // CA-01: mutación desde backoffice rechazada
    @Test
    void requireSlotOperable_rechazaBackoffice() {
        CustomException ex = assertThrows(CustomException.class,
                () -> service.requireSlotOperable("slot-uuid", 1L, false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private Slot slotActivo() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setStatus(0);

        Slot slot = new Slot();
        slot.setUuid("slot-uuid");
        slot.setActividad(actividad);
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slot.setInicio(LocalDateTime.now().plusDays(1));
        slot.setFin(LocalDateTime.now().plusDays(1).plusHours(1));
        return slot;
    }

    private void configurarProfesorAsignado(Long companyId, Long actividadId, Long profesorId) {
        Profesor profesor = new Profesor();
        profesor.setId(profesorId);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(companyId)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(actividadId, profesorId, 0)).thenReturn(true);
    }
}
