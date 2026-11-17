package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.DTO.EventoActividadEstadoCommand;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoActividadBajaServiceTest {

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

    @Mock
    private EventoActividadRepository eventoActividadRepository;

    private StubAuthService stubAuthService;
    private EventoActividadBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        EventoActividadAutorizacionService autorizacionService = new EventoActividadAutorizacionService();
        ReflectionTestUtils.setField(autorizacionService, "authService", stubAuthService);
        ReflectionTestUtils.setField(autorizacionService, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(autorizacionService, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorAuthService", profesorAuthService);
        ReflectionTestUtils.setField(autorizacionService, "memberAuthService", memberAuthService);

        service = new EventoActividadBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(service, "eventoActividadRepository", eventoActividadRepository);
        ReflectionTestUtils.setField(service, "eventoActividadMapper", new EventoActividadMapper());
    }

    // CA-03: cancelación mantiene status=0 y pasa a CANCELADO
    @Test
    void cambiarEstado_aCanceladoMantieneStatusActivo() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        EventoActividad evento = eventoActivo();
        when(eventoActividadRepository.findByUuidAndCompanyIdWithDetails("evento-uuid", 1L))
                .thenReturn(Optional.of(evento));
        when(profesorRepository.findByUserIdAndCompanyIdWithUser(any(), any())).thenReturn(Optional.empty());
        when(eventoActividadRepository.save(any(EventoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoActividadEstadoCommand command = new EventoActividadEstadoCommand();
        command.setEstado(EventoActividadEstadoEnum.CANCELADO);
        service.cambiarEstado("evento-uuid", command, false);

        ArgumentCaptor<EventoActividad> captor = ArgumentCaptor.forClass(EventoActividad.class);
        verify(eventoActividadRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(EventoActividadEstadoEnum.CANCELADO, captor.getValue().getEstado());
    }

    // CA-03: cambiar a un estado igual al actual es un conflicto
    @Test
    void cambiarEstado_mismoEstadoLanzaConflicto() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        EventoActividad evento = eventoActivo();
        when(eventoActividadRepository.findByUuidAndCompanyIdWithDetails("evento-uuid", 1L))
                .thenReturn(Optional.of(evento));
        when(profesorRepository.findByUserIdAndCompanyIdWithUser(any(), any())).thenReturn(Optional.empty());

        EventoActividadEstadoCommand command = new EventoActividadEstadoCommand();
        command.setEstado(EventoActividadEstadoEnum.CONFIRMADO);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.cambiarEstado("evento-uuid", command, false));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // CA-03: baja lógica impide consultas posteriores (status -1)
    @Test
    void bajaLogica_marcaStatusMenosUno() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        EventoActividad evento = eventoActivo();
        when(eventoActividadRepository.findByUuidAndCompanyIdWithDetails("evento-uuid", 1L))
                .thenReturn(Optional.of(evento));
        when(profesorRepository.findByUserIdAndCompanyIdWithUser(any(), any())).thenReturn(Optional.empty());
        when(eventoActividadRepository.save(any(EventoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        service.bajaLogica("evento-uuid", false);

        ArgumentCaptor<EventoActividad> captor = ArgumentCaptor.forClass(EventoActividad.class);
        verify(eventoActividadRepository).save(captor.capture());
        assertEquals(-1, captor.getValue().getStatus());
    }

    // CA-03: evento pasado permite cancelación
    @Test
    void cambiarEstado_eventoPasadoPermitido() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        EventoActividad evento = eventoActivo();
        evento.setInicio(LocalDateTime.now().minusDays(5));
        evento.setFin(LocalDateTime.now().minusDays(5).plusHours(1));

        when(eventoActividadRepository.findByUuidAndCompanyIdWithDetails("evento-pasado", 1L))
                .thenReturn(Optional.of(evento));
        when(profesorRepository.findByUserIdAndCompanyIdWithUser(any(), any())).thenReturn(Optional.empty());
        when(eventoActividadRepository.save(any(EventoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoActividadEstadoCommand command = new EventoActividadEstadoCommand();
        command.setEstado(EventoActividadEstadoEnum.CANCELADO);
        service.cambiarEstado("evento-pasado", command, false);

        verify(eventoActividadRepository).save(any(EventoActividad.class));
    }

    private EventoActividad eventoActivo() {
        Actividad actividad = new Actividad();
        actividad.setId(5L);
        actividad.setUuid("actividad-uuid");

        EventoActividad evento = new EventoActividad();
        evento.setUuid("evento-uuid");
        evento.setStatus(0);
        evento.setEstado(EventoActividadEstadoEnum.CONFIRMADO);
        evento.setActividad(actividad);
        evento.setInicio(LocalDateTime.now().plusDays(1));
        evento.setFin(LocalDateTime.now().plusDays(1).plusHours(1));
        return evento;
    }
}
