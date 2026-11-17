package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadBajaServiceTest {

    private static final Long COMPANY_ID = 1L;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private SlotRepository slotRepository;

    private StubAuthService stubAuthService;
    private ActividadBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        Company company = new Company();
        company.setId(COMPANY_ID);
        stubAuthService.setCompany(company);

        service = new ActividadBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
    }

    @Test
    void bajaLogica_sinInscriptos_desasignaProfesoresBorraSlotsFuturosYMarcaEliminada() {
        Actividad actividad = new Actividad();
        actividad.setId(5L);
        actividad.setUuid("act-uuid");
        actividad.setStatus(0);

        when(actividadRepository.findByUuidAndCompany_Id("act-uuid", COMPANY_ID)).thenReturn(Optional.of(actividad));
        when(inscripcionActividadRepository.countByActividad_IdAndStatus(5L, 0)).thenReturn(0L);

        service.bajaLogica("act-uuid");

        verify(actividadProfesorRepository).bajaLogicaByActividadId(5L);
        verify(slotRepository).deleteByActividad_IdAndInicioGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any());
        assertEquals(-1, actividad.getStatus());
        verify(actividadRepository).save(actividad);
    }

    @Test
    void bajaLogica_conInscriptosActivos_lanzaConflict() {
        Actividad actividad = new Actividad();
        actividad.setId(5L);
        actividad.setUuid("act-uuid");
        actividad.setStatus(0);

        when(actividadRepository.findByUuidAndCompany_Id("act-uuid", COMPANY_ID)).thenReturn(Optional.of(actividad));
        when(inscripcionActividadRepository.countByActividad_IdAndStatus(5L, 0)).thenReturn(2L);

        CustomException ex = assertThrows(CustomException.class, () -> service.bajaLogica("act-uuid"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(actividadProfesorRepository, never()).bajaLogicaByActividadId(ArgumentMatchers.anyLong());
        verify(slotRepository, never()).deleteByActividad_IdAndInicioGreaterThanEqual(
                ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        verify(actividadRepository, never()).save(actividad);
        assertEquals(0, actividad.getStatus());
    }

    @Test
    void bajaLogica_idempotenteSiYaEliminada() {
        Actividad actividad = new Actividad();
        actividad.setId(5L);
        actividad.setUuid("act-uuid");
        actividad.setStatus(-1);

        when(actividadRepository.findByUuidAndCompany_Id("act-uuid", COMPANY_ID)).thenReturn(Optional.of(actividad));

        service.bajaLogica("act-uuid");

        verify(inscripcionActividadRepository, never()).countByActividad_IdAndStatus(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt());
        verify(actividadProfesorRepository, never()).bajaLogicaByActividadId(ArgumentMatchers.anyLong());
        verify(actividadRepository, never()).save(actividad);
    }

    @Test
    void bajaLogica_actividadInexistente_lanzaNotFound() {
        when(actividadRepository.findByUuidAndCompany_Id("missing", COMPANY_ID)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.bajaLogica("missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
