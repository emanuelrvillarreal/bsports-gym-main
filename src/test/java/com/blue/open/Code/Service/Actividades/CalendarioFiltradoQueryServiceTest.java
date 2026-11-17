package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Entity.DTO.CalendarioSlotTipoEnum;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioFiltradoQueryServiceTest {

    @Mock
    private MemberAuthService memberAuthService;

    @Mock
    private ProfesorAuthService profesorAuthService;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private EventoActividadRepository eventoActividadRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AsistenciaActividadRepository asistenciaActividadRepository;

    private StubAuthService stubAuthService;
    private CalendarioFiltradoQueryService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new CalendarioFiltradoQueryService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "profesorAuthService", profesorAuthService);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "eventoActividadRepository", eventoActividadRepository);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "asistenciaActividadRepository", asistenciaActividadRepository);
        ReflectionTestUtils.setField(service, "eventoActividadMapper", new EventoActividadMapper());
    }

    // CA-06: socio inscripto ve solo eventos de sus actividades
    @Test
    void listarSocio_filtraEventosPorActividadesInscriptas() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Member member = new Member();
        member.setId(50L);
        when(memberAuthService.getCurrentMemberOrThrow(1L)).thenReturn(member);
        when(inscripcionActividadRepository.findActividadIdsByMemberIdAndCompanyId(50L, 1L))
                .thenReturn(List.of(10L));

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(slotRepository.findByActividadIdsAndInicioBetweenWithDetails(1L, List.of(10L), desde, hasta))
                .thenReturn(List.of());

        Actividad actividad = new Actividad();
        actividad.setUuid("act-a");
        actividad.setNombre("Actividad A");

        EventoActividad evento = new EventoActividad();
        evento.setUuid("ev-a");
        evento.setActividad(actividad);
        evento.setTitulo("Clase especial");
        evento.setUbicacion("Sala");
        evento.setInicio(desde.plusDays(5));
        evento.setFin(desde.plusDays(5).plusHours(1));
        evento.setEstado(EventoActividadEstadoEnum.CONFIRMADO);

        when(eventoActividadRepository.findByActividadIdsAndInicioBetweenWithDetails(
                eq(1L), eq(List.of(10L)), eq(desde), eq(hasta)))
                .thenReturn(List.of(evento));
        when(reservaRepository.findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
                eq(1L), eq(50L), any(), eq(desde), eq(hasta))).thenReturn(List.of());

        List<CalendarioSlotDTO> resultado = service.listarSocio(desde, hasta);

        assertEquals(1, resultado.size());
        assertEquals("ev-a", resultado.get(0).getEventoUuid());
        assertEquals("Actividad A", resultado.get(0).getReferenciaNombre());
    }

    // CA-06: socio sin inscripciones no ve eventos de actividades
    @Test
    void listarSocio_sinInscripcionesRetornaVacio() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Member member = new Member();
        member.setId(50L);
        when(memberAuthService.getCurrentMemberOrThrow(1L)).thenReturn(member);
        when(inscripcionActividadRepository.findActividadIdsByMemberIdAndCompanyId(50L, 1L))
                .thenReturn(List.of());

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(reservaRepository.findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
                eq(1L), eq(50L), any(), eq(desde), eq(hasta))).thenReturn(List.of());

        List<CalendarioSlotDTO> resultado = service.listarSocio(desde, hasta);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarSocio_incluyeReservasAmenityDelTitularGrupo() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Member titular = new Member();
        titular.setId(10L);
        User titularUser = new User();
        titularUser.setNombre("Juan");
        titularUser.setApellido("Perez");
        titular.setUser(titularUser);

        Member familiar = new Member();
        familiar.setId(50L);
        familiar.setTitular(titular);

        when(memberAuthService.getCurrentMemberOrThrow(1L)).thenReturn(familiar);
        when(inscripcionActividadRepository.findActividadIdsByMemberIdAndCompanyId(50L, 1L))
                .thenReturn(List.of());

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);
        LocalDateTime inicioSlot = desde.plusDays(3).withHour(10);

        Amenity amenity = new Amenity();
        amenity.setUuid("amenity-uuid");
        amenity.setNombre("Parrilla");

        Slot slot = new Slot();
        slot.setUuid("slot-uuid");
        slot.setInicio(inicioSlot);
        slot.setFin(inicioSlot.plusHours(2));
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);

        Reserva reserva = new Reserva();
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);
        reserva.setMember(titular);
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);

        when(reservaRepository.findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
                eq(1L), eq(10L), any(), eq(desde), eq(hasta))).thenReturn(List.of(reserva));

        List<CalendarioSlotDTO> resultado = service.listarSocio(desde, hasta);

        assertEquals(1, resultado.size());
        assertEquals(CalendarioSlotTipoEnum.AMENITY, resultado.get(0).getTipo());
        assertEquals("amenity-uuid", resultado.get(0).getReferenciaUuid());
        assertEquals("Parrilla", resultado.get(0).getReferenciaNombre());
        assertEquals("Juan", resultado.get(0).getTitularNombre());
    }

    // El socio solo ve las clases de los horarios que eligio.
    @Test
    void listarSocio_muestraSoloClasesDeSusHorarios() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Member member = new Member();
        member.setId(50L);
        when(memberAuthService.getCurrentMemberOrThrow(1L)).thenReturn(member);
        when(inscripcionActividadRepository.findActividadIdsByMemberIdAndCompanyId(50L, 1L))
                .thenReturn(List.of(10L));
        when(inscripcionActividadRepository.findHorarioUuidsActivosByMemberIdAndCompanyId(50L, 1L))
                .thenReturn(List.of("hor-lunes"));

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        Actividad actividad = new Actividad();
        actividad.setUuid("act-a");
        actividad.setNombre("Actividad A");
        Slot lunes = slotActividad("slot-lunes", actividad, horario("hor-lunes"), LocalDateTime.of(2026, 6, 1, 18, 0));
        Slot miercoles = slotActividad("slot-miercoles", actividad, horario("hor-miercoles"),
                LocalDateTime.of(2026, 6, 3, 18, 0));

        when(slotRepository.findByActividadIdsAndInicioBetweenWithDetails(1L, List.of(10L), desde, hasta))
                .thenReturn(List.of(lunes, miercoles));
        when(eventoActividadRepository.findByActividadIdsAndInicioBetweenWithDetails(
                eq(1L), eq(List.of(10L)), eq(desde), eq(hasta))).thenReturn(List.of());
        when(reservaRepository.findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
                eq(1L), eq(50L), any(), eq(desde), eq(hasta))).thenReturn(List.of());

        List<CalendarioSlotDTO> resultado = service.listarSocio(desde, hasta);

        assertEquals(1, resultado.size());
        assertEquals("slot-lunes", resultado.get(0).getSlotUuid());
        assertEquals("hor-lunes", resultado.get(0).getHorarioUuid());
    }

    // El porcentaje de asistencia de cada clase se calcula sobre los alumnos de su horario.
    @Test
    void listarProfesor_porcentajeAsistenciaPorHorario() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Profesor profesor = new Profesor();
        profesor.setId(5L);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.findActividadIdsByProfesorId(5L)).thenReturn(List.of(10L));

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        Actividad actividad = new Actividad();
        actividad.setUuid("act-a");
        actividad.setNombre("Actividad A");
        Slot lunes = slotActividad("slot-lunes", actividad, horario("hor-lunes"), LocalDateTime.of(2026, 6, 1, 18, 0));
        Slot miercoles = slotActividad("slot-miercoles", actividad, horario("hor-miercoles"),
                LocalDateTime.of(2026, 6, 3, 18, 0));
        when(slotRepository.findByActividadIdsAndInicioBetweenWithDetails(1L, List.of(10L), desde, hasta))
                .thenReturn(List.of(lunes, miercoles));
        when(eventoActividadRepository.findByActividadIdsAndInicioBetweenWithDetails(
                eq(1L), eq(List.of(10L)), eq(desde), eq(hasta))).thenReturn(List.of());

        List<Object[]> presentes = new ArrayList<>();
        presentes.add(new Object[]{"slot-lunes", 1L});
        when(asistenciaActividadRepository.countBySlotUuidInAndEstado(
                Set.of("slot-lunes", "slot-miercoles"), AsistenciaActividadEstadoEnum.PRESENTE))
                .thenReturn(presentes);

        // Lunes: 2 alumnos (uno con ambos horarios). Miercoles: solo el que tiene ambos.
        Timestamp actualizado = Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 0, 0));
        List<Object[]> alumnos = new ArrayList<>();
        alumnos.add(new Object[]{"act-a", 100L, 0, actualizado, "hor-lunes"});
        alumnos.add(new Object[]{"act-a", 101L, 0, actualizado, "hor-lunes"});
        alumnos.add(new Object[]{"act-a", 101L, 0, actualizado, "hor-miercoles"});
        when(inscripcionActividadRepository.findAlumnosByActividadUuidIn(eq(1L), eq(Set.of("act-a")), any()))
                .thenReturn(alumnos);

        List<CalendarioSlotDTO> resultado = service.listarProfesor(desde, hasta);

        assertEquals(2, resultado.size());
        assertEquals("slot-lunes", resultado.get(0).getSlotUuid());
        assertEquals(Double.valueOf(50.0), resultado.get(0).getPorcentajeAsistencia());
        assertEquals("slot-miercoles", resultado.get(1).getSlotUuid());
        assertEquals(Double.valueOf(0.0), resultado.get(1).getPorcentajeAsistencia());
    }

    private ActividadHorario horario(String uuid) {
        ActividadHorario horario = new ActividadHorario();
        horario.setUuid(uuid);
        return horario;
    }

    private Slot slotActividad(String uuid, Actividad actividad, ActividadHorario horario, LocalDateTime inicio) {
        Slot slot = new Slot();
        slot.setUuid(uuid);
        slot.setActividad(actividad);
        slot.setHorario(horario);
        slot.setInicio(inicio);
        slot.setFin(inicio.plusHours(1));
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        return slot;
    }
}
