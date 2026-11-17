package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.EventoActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoActividadAltaServiceTest {

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
    private EventoActividadAltaService service;

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

        service = new EventoActividadAltaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(service, "eventoActividadRepository", eventoActividadRepository);
        ReflectionTestUtils.setField(service, "eventoActividadMapper", new EventoActividadMapper());
    }

    // CA-01: alta válida persiste evento CONFIRMADO con status=0 en evento_actividad
    @Test
    void crear_persisteEventoConfirmadoVinculadoAActividadYCompany() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setUuid("actividad-uuid");
        actividad.setNombre("Fútbol");

        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(2);

        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(profesorRepository.findByUserIdAndCompanyIdWithUser(any(), any())).thenReturn(Optional.empty());
        when(eventoActividadRepository.save(any(EventoActividad.class))).thenAnswer(inv -> {
            EventoActividad evento = inv.getArgument(0);
            evento.setId(99L);
            evento.setUuid("evento-uuid");
            return evento;
        });

        EventoActividadAltaCommand command = new EventoActividadAltaCommand();
        command.setActividadUuid("actividad-uuid");
        command.setTitulo("Torneo");
        command.setDescripcion("Descripción del torneo");
        command.setUbicacion("Cancha 1");
        command.setInicio(inicio);
        command.setFin(fin);

        EventoActividadDetalleDTO resultado = service.crear(command, false);

        ArgumentCaptor<EventoActividad> captor = ArgumentCaptor.forClass(EventoActividad.class);
        verify(eventoActividadRepository).save(captor.capture());
        EventoActividad guardado = captor.getValue();

        assertEquals(0, guardado.getStatus());
        assertEquals(EventoActividadEstadoEnum.CONFIRMADO, guardado.getEstado());
        assertEquals(company, guardado.getCompany());
        assertEquals(actividad, guardado.getActividad());
        assertEquals("Torneo", guardado.getTitulo());
        assertEquals("evento-uuid", resultado.getUuid());
        assertEquals(EventoActividadEstadoEnum.CONFIRMADO, resultado.getEstado());
    }

    // CA-05: solapamientos permitidos — alta no valida conflictos con slots/eventos
    @Test
    void crear_noRechazaPorSolapamientoConOtrosHorarios() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
        stubAuthService.setUser(new com.blue.open.Code.Entity.Db.User());

        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setUuid("actividad-uuid");

        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(1);

        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        com.blue.open.Code.Entity.Db.Profesor profesor = new com.blue.open.Code.Entity.Db.Profesor();
        profesor.setId(3L);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 3L, 0)).thenReturn(true);
        when(eventoActividadRepository.save(any(EventoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoActividadAltaCommand command = new EventoActividadAltaCommand();
        command.setActividadUuid("actividad-uuid");
        command.setTitulo("Evento solapado");
        command.setDescripcion("Desc");
        command.setUbicacion("Ubic");
        command.setInicio(inicio);
        command.setFin(fin);

        service.crear(command, true);

        verify(eventoActividadRepository).save(any(EventoActividad.class));
    }
}
