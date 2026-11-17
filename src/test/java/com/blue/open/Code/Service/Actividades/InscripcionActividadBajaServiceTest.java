package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
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
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionActividadBajaServiceTest {

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

    private StubAuthService stubAuthService;
    private InscripcionActividadBajaService service;

    private Company company;
    private Actividad actividad;
    private Member member;
    private ActividadInscripcionCommand command;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new InscripcionActividadBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "memberCategoriaInscripcionService", memberCategoriaInscripcionService);

        ActividadHorariosService actividadHorariosService = new ActividadHorariosService();
        ReflectionTestUtils.setField(actividadHorariosService, "actividadHorarioRepository", actividadHorarioRepository);
        ReflectionTestUtils.setField(actividadHorariosService, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "actividadHorariosService", actividadHorariosService);

        company = new Company();
        company.setId(10L);
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

        command = new ActividadInscripcionCommand();
        command.setMemberUuid("member-uuid");
    }

    @Test
    void darBaja_inscripcionActiva_liberaCupoYMarcaIncompleta() {
        actividad.setCompleta(true);

        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setUuid("ins-baja");
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.setStatus(0);

        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.findByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(Optional.of(inscripcion));
        when(inscripcionActividadRepository.save(inscripcion)).thenReturn(inscripcion);
        stubHorarioConInscriptos(2L);
        when(actividadRepository.save(actividad)).thenReturn(actividad);

        InscripcionActividadDTO dto = service.darBaja("act-uuid", command);

        assertEquals(-1, inscripcion.getStatus());
        assertFalse(actividad.isCompleta());
        assertFalse(dto.isActividadCompleta());
        verify(actividadRepository).save(eq(actividad));
        verify(memberCategoriaInscripcionService).desasignarSiCorresponde(member, actividad);
    }

    @Test
    void darBaja_otroHorarioSigueConLugar_actividadSigueIncompleta() {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setUuid("ins-baja");
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.setStatus(0);

        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.findByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(Optional.of(inscripcion));
        when(inscripcionActividadRepository.save(inscripcion)).thenReturn(inscripcion);
        stubHorarioConInscriptos(1L);

        InscripcionActividadDTO dto = service.darBaja("act-uuid", command);

        assertFalse(dto.isActividadCompleta());
        // Ya estaba incompleta: no hace falta volver a guardar la actividad.
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void darBaja_sinInscripcionActiva_lanzaNotFound() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.of(actividad));
        when(memberAuthService.resolveMemberByUuid("member-uuid", 10L)).thenReturn(member);
        when(inscripcionActividadRepository.findByActividad_IdAndMember_IdAndStatus(1L, 100L, 0))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.darBaja("act-uuid", command));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("El socio no está inscripto en esta actividad", ex.getUserMessage());
        verify(inscripcionActividadRepository, never()).save(any());
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void darBaja_actividadNoEncontrada_lanzaNotFound() {
        when(actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate("act-uuid", 10L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.darBaja("act-uuid", command));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(inscripcionActividadRepository, never()).save(any());
    }

    private void stubHorarioConInscriptos(long inscriptos) {
        ActividadHorario horario = new ActividadHorario();
        horario.setId(7L);
        horario.setUuid("hor-lunes");
        horario.setActividad(actividad);
        horario.setDiaSemana(DiaSemanaEnum.LUNES);
        horario.setHoraInicio(LocalTime.of(18, 0));
        horario.setHoraFin(LocalTime.of(19, 0));
        horario.setCupoMaximo(3);
        List<Object[]> conteo = new ArrayList<>();
        conteo.add(new Object[]{7L, inscriptos});
        when(actividadHorarioRepository.findActivosByActividadId(1L)).thenReturn(List.of(horario));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(List.of(7L))).thenReturn(conteo);
    }

    @Test
    void bajaTodasActivasPorMember_sinInscripciones_noPersiste() {
        when(inscripcionActividadRepository.findActivasByMemberIdAndCompanyIdWithActividad(100L, 10L, 0))
                .thenReturn(List.of());

        service.bajaTodasActivasPorMember(100L, 10L);

        verify(inscripcionActividadRepository, never()).saveAll(any());
        verify(actividadRepository, never()).findByIdAndCompany_IdAndStatusForUpdate(any(), any());
    }

    @Test
    void bajaTodasActivasPorMember_conInscripciones_bajaYSincronizaCupo() {
        actividad.setCompleta(true);

        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setUuid("ins-baja-masiva");
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.setStatus(0);

        when(inscripcionActividadRepository.findActivasByMemberIdAndCompanyIdWithActividad(100L, 10L, 0))
                .thenReturn(List.of(inscripcion));
        when(inscripcionActividadRepository.saveAll(List.of(inscripcion))).thenReturn(List.of(inscripcion));
        when(actividadRepository.findByIdAndCompany_IdAndStatusForUpdate(1L, 10L))
                .thenReturn(Optional.of(actividad));
        stubHorarioConInscriptos(2L);
        when(actividadRepository.save(actividad)).thenReturn(actividad);

        service.bajaTodasActivasPorMember(100L, 10L);

        assertEquals(-1, inscripcion.getStatus());
        assertFalse(actividad.isCompleta());
        verify(inscripcionActividadRepository).saveAll(List.of(inscripcion));
        verify(actividadRepository).save(eq(actividad));
        verify(memberCategoriaInscripcionService).limpiar(member);
    }
}
