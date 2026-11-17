package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaDTO;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AsistenciaActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Code.Entity.Db.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaPlanillaQueryServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ProfesorAuthService profesorAuthService;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private AsistenciaActividadRepository asistenciaActividadRepository;

    private AsistenciaPlanillaQueryService service;
    private StubAuthService stubAuthService;
    private AsistenciaAutorizacionService autorizacionService;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        autorizacionService = new AsistenciaAutorizacionService();
        ReflectionTestUtils.setField(autorizacionService, "authService", stubAuthService);
        ReflectionTestUtils.setField(autorizacionService, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(autorizacionService, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorAuthService", profesorAuthService);

        service = new AsistenciaPlanillaQueryService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "asistenciaActividadRepository", asistenciaActividadRepository);
        ReflectionTestUtils.setField(service, "asistenciaActividadMapper", new AsistenciaActividadMapper());
    }

    // CA-02: slot futuro lista solo inscripciones activas
    @Test
    void obtenerPlanilla_slotFuturo_soloActivas() {
        configurarProfesorAsignado();
        Slot slot = slotConFecha(LocalDate.now().plusDays(2));
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(inscripcion(1L, "member-1", 0, LocalDate.now().minusMonths(1))));
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of());

        AsistenciaPlanillaDTO planilla = service.obtenerPlanilla("slot-uuid");

        assertEquals(1, planilla.getAlumnos().size());
        assertEquals("member-1", planilla.getAlumnos().get(0).getMemberUuid());
    }

    // CA-02: slot pasado incluye activas y ex-inscriptos
    @Test
    void obtenerPlanilla_slotPasado_incluyeHistoricas() {
        configurarProfesorAsignado();
        Slot slot = slotConFecha(LocalDate.now().minusDays(5));
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findByActividadIdAndCompanyIdAndStatusInWithMember(
                eq(10L), eq(1L), any()))
                .thenReturn(List.of(
                        inscripcion(1L, "member-activo", 0, LocalDate.now().minusMonths(2)),
                        inscripcion(2L, "member-baja", -1, LocalDate.now().minusMonths(3))));
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of());

        AsistenciaPlanillaDTO planilla = service.obtenerPlanilla("slot-uuid");

        assertEquals(2, planilla.getAlumnos().size());
    }

    // CA-02: inscripción posterior al slot figura con asistencia null
    @Test
    void obtenerPlanilla_inscripcionPosterior_muestraEstadoNull() {
        configurarProfesorAsignado();
        LocalDate fechaSlot = LocalDate.now().minusDays(3);
        Slot slot = slotConFecha(fechaSlot);
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findByActividadIdAndCompanyIdAndStatusInWithMember(
                eq(10L), eq(1L), any()))
                .thenReturn(List.of(inscripcion(1L, "member-tarde", 0, fechaSlot.plusDays(1))));
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of());

        AsistenciaPlanillaDTO planilla = service.obtenerPlanilla("slot-uuid");

        assertNull(planilla.getAlumnos().get(0).getEstado());
    }

    // CA-02: merge asistencia persistida
    @Test
    void obtenerPlanilla_muestraAsistenciaPersistida() {
        configurarProfesorAsignado();
        Slot slot = slotConFecha(LocalDate.now());
        Member member = member(1L, "member-1");
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(inscripcion(member, 0, LocalDate.now().minusMonths(1))));

        AsistenciaActividad asistencia = new AsistenciaActividad();
        asistencia.setMember(member);
        asistencia.setEstado(AsistenciaActividadEstadoEnum.PRESENTE);
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of(asistencia));

        AsistenciaPlanillaDTO planilla = service.obtenerPlanilla("slot-uuid");

        assertEquals(AsistenciaActividadEstadoEnum.PRESENTE, planilla.getAlumnos().get(0).getEstado());
    }

    // CA-02: dedupe por member ante re-inscripciones
    @Test
    void cargarInscripcionesPlanilla_deduplicaPorMember() {
        Slot slot = slotConFecha(LocalDate.now().minusDays(1));
        when(inscripcionActividadRepository.findByActividadIdAndCompanyIdAndStatusInWithMember(
                eq(10L), eq(1L), any()))
                .thenReturn(List.of(
                        inscripcion(1L, "member-1", -1, LocalDate.of(2024, 1, 1)),
                        inscripcion(1L, "member-1", 0, LocalDate.of(2024, 6, 1))));

        List<InscripcionActividad> inscripciones = service.cargarInscripcionesPlanilla(slot, 1L);

        assertEquals(1, inscripciones.size());
        assertEquals(0, inscripciones.get(0).getStatus());
    }

    // Cada clase sale de un horario: solo figuran quienes eligieron ese horario.
    @Test
    void cargarInscripcionesPlanilla_slotConHorario_filtraPorHorarioElegido() {
        ActividadHorario lunes = horario(7L);
        ActividadHorario miercoles = horario(8L);
        Slot slot = slotConFecha(LocalDate.now().plusDays(1));
        slot.setHorario(lunes);

        InscripcionActividad delLunes = inscripcion(1L, "member-lunes", 0, LocalDate.now().minusMonths(1));
        delLunes.getHorarios().add(lunes);
        InscripcionActividad delMiercoles = inscripcion(2L, "member-miercoles", 0, LocalDate.now().minusMonths(1));
        delMiercoles.getHorarios().add(miercoles);
        InscripcionActividad deAmbos = inscripcion(3L, "member-ambos", 0, LocalDate.now().minusMonths(1));
        deAmbos.getHorarios().add(miercoles);
        deAmbos.getHorarios().add(lunes);
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(delLunes, delMiercoles, deAmbos));

        List<InscripcionActividad> inscripciones = service.cargarInscripcionesPlanilla(slot, 1L);

        assertEquals(List.of(delLunes, deAmbos), inscripciones);
    }

    private ActividadHorario horario(Long id) {
        ActividadHorario horario = new ActividadHorario();
        horario.setId(id);
        horario.setUuid("hor-" + id);
        return horario;
    }

    private void configurarProfesorAsignado() {
        Profesor profesor = new Profesor();
        profesor.setId(5L);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 5L, 0)).thenReturn(true);
    }

    private Slot slotConFecha(LocalDate fecha) {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setUuid("actividad-uuid");
        actividad.setNombre("Yoga");
        actividad.setStatus(0);

        Slot slot = new Slot();
        slot.setId(100L);
        slot.setUuid("slot-uuid");
        slot.setActividad(actividad);
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slot.setInicio(fecha.atTime(10, 0));
        slot.setFin(fecha.atTime(11, 0));
        return slot;
    }

    private InscripcionActividad inscripcion(Long memberId, String memberUuid, int status, LocalDate fechaInscripcion) {
        return inscripcion(member(memberId, memberUuid), status, fechaInscripcion);
    }

    private InscripcionActividad inscripcion(Member member, int status, LocalDate fechaInscripcion) {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setMember(member);
        inscripcion.setStatus(status);
        inscripcion.setFechaInscripcion(fechaInscripcion);
        return inscripcion;
    }

    private Member member(Long id, String uuid) {
        Member member = new Member();
        member.setId(id);
        member.setUuid(uuid);
        User user = new User();
        user.setNombre("Nombre");
        user.setApellido("Apellido");
        member.setUser(user);
        return member;
    }
}
