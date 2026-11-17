package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.ActividadHorarioRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.support.RecordingCuotaActividadGeneracionService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadInscripcionServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private MemberAuthService memberAuthService;

    @Mock
    private MemberCategoriaInscripcionService memberCategoriaInscripcionService;

    @Mock
    private ActividadHorarioRepository actividadHorarioRepository;

    private RecordingCuotaActividadGeneracionService recordingCuotaActividadService;

    private StubAuthService stubAuthService;
    private ActividadInscripcionService service;

    private Company company;
    private Actividad actividad;
    private Member member;
    private ActividadInscripcionCommand command;
    private ActividadHorario horarioLunes;
    private ActividadHorario horarioMiercoles;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        recordingCuotaActividadService = new RecordingCuotaActividadGeneracionService();
        service = new ActividadInscripcionService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "cuotaActividadGeneracionService", recordingCuotaActividadService);
        ReflectionTestUtils.setField(service, "memberCategoriaInscripcionService", memberCategoriaInscripcionService);

        // Servicio real de horarios: el cupo se valida por horario contra el repositorio mockeado.
        ActividadHorariosService actividadHorariosService = new ActividadHorariosService();
        ReflectionTestUtils.setField(actividadHorariosService, "actividadHorarioRepository", actividadHorarioRepository);
        ReflectionTestUtils.setField(actividadHorariosService, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "actividadHorariosService", actividadHorariosService);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        stubAuthService.setCompany(company);

        actividad = new Actividad();
        actividad.setId(1L);
        actividad.setUuid("act-uuid");
        actividad.setNombre("Yoga");
        actividad.setCompany(company);
        actividad.setCupoMaximo(3);
        actividad.setCompleta(false);

        User user = new User();
        user.setNombre("Ana");
        user.setApellido("García");

        member = new Member();
        member.setId(100L);
        member.setUuid("member-uuid");
        member.setEstado(MemberEstadoEnum.ACTIVO);
        member.setUser(user);
        member.setCompany(company);

        horarioLunes = horario(7L, "hor-lunes", DiaSemanaEnum.LUNES, 3);
        horarioMiercoles = horario(8L, "hor-miercoles", DiaSemanaEnum.MIERCOLES, 3);

        command = new ActividadInscripcionCommand();
        command.setMemberUuid("member-uuid");
        command.setHorarioUuids(List.of("hor-lunes"));
    }

    @Test
    void inscribir_titularActivo_creaInscripcion() {
        actividad.setPeriodicidadCuota(PeriodicidadCuotaEnum.SEMANAL);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        stubHorarios(List.of(horarioLunes), inscriptosLunes(0L), inscriptosLunes(1L));
        when(inscripcionActividadRepository.save(any(InscripcionActividad.class))).thenAnswer(inv -> {
            InscripcionActividad ins = inv.getArgument(0);
            ins.setUuid("ins-uuid");
            ins.setFechaInscripcion(LocalDate.now());
            return ins;
        });
        InscripcionActividadDTO dto = service.inscribir("act-uuid", command);

        assertEquals("ins-uuid", dto.getInscripcionUuid());
        assertTrue(dto.isCuotaGenerada());
        assertEquals("cuota-uuid", dto.getCuotaUuid());
        assertEquals("act-uuid", dto.getActividadUuid());
        assertEquals("Yoga", dto.getActividadNombre());
        assertEquals("member-uuid", dto.getMemberUuid());
        assertEquals("Ana", dto.getMemberNombre());
        assertFalse(dto.isActividadCompleta());
        assertEquals(List.of("hor-lunes"), dto.getHorarioUuids());

        ArgumentCaptor<InscripcionActividad> captor = ArgumentCaptor.forClass(InscripcionActividad.class);
        verify(inscripcionActividadRepository).save(captor.capture());
        assertEquals(actividad, captor.getValue().getActividad());
        assertEquals(member, captor.getValue().getMember());
        assertEquals(Set.of(horarioLunes), captor.getValue().getHorarios());
        // El alta ya genera la cuota de la fecha de inscripción: la próxima queda un ciclo después.
        assertEquals(captor.getValue().getFechaInscripcion().plusDays(7), captor.getValue().getProximaGeneracion());
        assertEquals(100L, recordingCuotaActividadService.getLastMemberId());
        assertEquals(1L, recordingCuotaActividadService.getLastActividadId());
        assertEquals(captor.getValue().getFechaInscripcion(), recordingCuotaActividadService.getLastFecha());
        verify(memberCategoriaInscripcionService).asignar(member, null);
    }

    @Test
    void inscribir_familiarActivo_creaInscripcion() {
        Member titular = new Member();
        titular.setId(99L);
        member.setTitular(titular);

        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        stubHorarios(List.of(horarioLunes), inscriptosLunes(1L), inscriptosLunes(2L));
        when(inscripcionActividadRepository.save(any(InscripcionActividad.class))).thenAnswer(inv -> {
            InscripcionActividad ins = inv.getArgument(0);
            ins.setUuid("ins-familiar");
            ins.setFechaInscripcion(LocalDate.now());
            return ins;
        });
        InscripcionActividadDTO dto = service.inscribir("act-uuid", command);

        assertEquals("ins-familiar", dto.getInscripcionUuid());
        verify(inscripcionActividadRepository).save(any(InscripcionActividad.class));
    }

    @Test
    void inscribir_categoriaConEdadEnRango_incluyeLimites() {
        asignarCategoriaConRango(24, 24);
        member.getUser().setFechaNacimiento(LocalDate.of(LocalDate.now().getYear() - 24, 10, 1));
        prepararInscripcionExitosa();

        InscripcionActividadDTO dto = service.inscribir("act-uuid", command);

        assertEquals("ins-uuid", dto.getInscripcionUuid());
        verify(inscripcionActividadRepository).save(any(InscripcionActividad.class));
    }

    @Test
    void inscribir_categoriaConEdadMenorAlRango_lanzaBadRequestSinInsertar() {
        asignarCategoriaConRango(24, 30);
        member.getUser().setFechaNacimiento(LocalDate.of(LocalDate.now().getYear() - 23, 10, 1));
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El socio no cumple el rango de edad de ninguna categoria de la actividad", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_categoriaConEdadMayorAlRango_lanzaBadRequestSinInsertar() {
        asignarCategoriaConRango(18, 24);
        member.getUser().setFechaNacimiento(LocalDate.of(LocalDate.now().getYear() - 25, 10, 1));
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El socio no cumple el rango de edad de ninguna categoria de la actividad", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_categoriaSinFechaNacimiento_lanzaBadRequestSinInsertar() {
        asignarCategoriaConRango(18, 65);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El socio no tiene fecha de nacimiento informada", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_yaInscripto_lanzaBadRequest() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El socio ya está inscripto en esta actividad", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_memberMoroso_lanzaBadRequest() {
        member.setEstado(MemberEstadoEnum.MOROSO);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getUserMessage().contains("MOROSO"));
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_memberInactivo_lanzaBadRequest() {
        member.setEstado(MemberEstadoEnum.INACTIVO);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getUserMessage().contains("INACTIVO"));
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_actividadNoEncontrada_lanzaNotFound() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_sinHorarios_lanzaBadRequestSinInsertar() {
        command.setHorarioUuids(List.of());
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Elegí al menos un horario", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_horarioDeOtraActividad_lanzaBadRequestSinInsertar() {
        command.setHorarioUuids(List.of("hor-ajeno"));
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        when(actividadHorarioRepository.findActivosByActividadId(1L)).thenReturn(List.of(horarioLunes));

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El horario elegido no pertenece a la actividad", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_horarioLleno_lanzaBadRequestSinInsertar() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        stubHorarios(List.of(horarioLunes), inscriptosLunes(3L));

        CustomException ex = assertThrows(CustomException.class, () -> service.inscribir("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("No quedan lugares en el horario del lunes de 18:00 a 19:00", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @Test
    void inscribir_otroHorarioConLugar_inscribeAunqueUnHorarioEsteLleno() {
        command.setHorarioUuids(List.of("hor-miercoles"));
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        // Lunes lleno (3/3), miercoles con lugar (1/3 antes, 2/3 despues).
        stubHorarios(List.of(horarioLunes, horarioMiercoles),
                filas(new Object[]{8L, 1L}),
                filas(new Object[]{7L, 3L}, new Object[]{8L, 2L}));
        when(inscripcionActividadRepository.save(any(InscripcionActividad.class))).thenAnswer(inv -> {
            InscripcionActividad ins = inv.getArgument(0);
            ins.setUuid("ins-miercoles");
            return ins;
        });

        InscripcionActividadDTO dto = service.inscribir("act-uuid", command);

        assertEquals("ins-miercoles", dto.getInscripcionUuid());
        assertEquals(List.of("hor-miercoles"), dto.getHorarioUuids());
        // No todos los horarios estan llenos: la actividad no queda completa.
        assertFalse(actividad.isCompleta());
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void inscribir_ultimoCupo_marcaActividadCompleta() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        stubHorarios(List.of(horarioLunes), inscriptosLunes(2L), inscriptosLunes(3L));
        when(inscripcionActividadRepository.save(any(InscripcionActividad.class))).thenAnswer(inv -> {
            InscripcionActividad ins = inv.getArgument(0);
            ins.setUuid("ins-ultimo");
            ins.setFechaInscripcion(LocalDate.now());
            return ins;
        });
        when(actividadRepository.save(actividad)).thenReturn(actividad);

        InscripcionActividadDTO dto = service.inscribir("act-uuid", command);

        assertTrue(actividad.isCompleta());
        assertTrue(dto.isActividadCompleta());
        verify(actividadRepository).save(eq(actividad));
    }

    @Test
    void cambiarHorarios_conservaSuHorarioLlenoYSumaOtro() {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setUuid("ins-uuid");
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.getHorarios().add(horarioLunes);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.findByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(Optional.of(inscripcion));
        // El lunes esta lleno pero uno de los lugares es del propio socio.
        stubHorarios(List.of(horarioLunes, horarioMiercoles),
                filas(new Object[]{7L, 3L}, new Object[]{8L, 0L}),
                filas(new Object[]{7L, 3L}, new Object[]{8L, 1L}));

        InscripcionActividadDTO dto = service.cambiarHorarios(
                "act-uuid", "member-uuid", List.of("hor-lunes", "hor-miercoles"));

        assertEquals(Set.of(horarioLunes, horarioMiercoles), inscripcion.getHorarios());
        assertEquals(List.of("hor-lunes", "hor-miercoles"), dto.getHorarioUuids());
        verify(inscripcionActividadRepository).save(inscripcion);
    }

    @Test
    void cambiarHorarios_aHorarioLleno_lanzaBadRequestSinModificar() {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.getHorarios().add(horarioLunes);
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.findByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(Optional.of(inscripcion));
        stubHorarios(List.of(horarioLunes, horarioMiercoles), filas(new Object[]{8L, 3L}));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.cambiarHorarios("act-uuid", "member-uuid", List.of("hor-miercoles")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Set.of(horarioLunes), inscripcion.getHorarios());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    @SafeVarargs
    private void stubHorarios(List<ActividadHorario> horarios, List<Object[]> primerConteo, List<Object[]>... siguientes) {
        when(actividadHorarioRepository.findActivosByActividadId(1L)).thenReturn(horarios);
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(primerConteo, siguientes);
    }

    private static List<Object[]> inscriptosLunes(long cantidad) {
        return filas(new Object[]{7L, cantidad});
    }

    private static List<Object[]> filas(Object[]... filas) {
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            resultado.add(fila);
        }
        return resultado;
    }

    private ActividadHorario horario(Long id, String uuid, DiaSemanaEnum dia, int cupo) {
        ActividadHorario horario = new ActividadHorario();
        horario.setId(id);
        horario.setUuid(uuid);
        horario.setActividad(actividad);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 0));
        horario.setCupoMaximo(cupo);
        return horario;
    }

    private void asignarCategoriaConRango(int edadMinima, int edadMaxima) {
        Categoria categoria = new Categoria();
        categoria.setId(50L);
        categoria.setNombre("Categoria " + edadMinima + "-" + edadMaxima);
        categoria.setEdadMinima(edadMinima);
        categoria.setEdadMaxima(edadMaxima);
        actividad.getCategorias().add(categoria);
    }

    private void prepararInscripcionExitosa() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(false);
        stubHorarios(List.of(horarioLunes), inscriptosLunes(0L), inscriptosLunes(1L));
        when(inscripcionActividadRepository.save(any(InscripcionActividad.class))).thenAnswer(inv -> {
            InscripcionActividad inscripcion = inv.getArgument(0);
            inscripcion.setUuid("ins-uuid");
            return inscripcion;
        });
    }

}
