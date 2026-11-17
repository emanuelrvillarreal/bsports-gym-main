package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.DTO.AsistenciaPaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Mapper.AsistenciaActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaConsultaQueryServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ProfesorAuthService profesorAuthService;

    @Mock
    private MemberAuthService memberAuthService;

    @Mock
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    private AsistenciaConsultaQueryService service;
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
        ReflectionTestUtils.setField(autorizacionService, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(autorizacionService, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorAuthService", profesorAuthService);

        service = new AsistenciaConsultaQueryService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "asistenciaActividadRepository", asistenciaActividadRepository);
        ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(service, "asistenciaActividadMapper", new AsistenciaActividadMapper());
    }

    @Test
    void paginar_sinActividadUuid_rechaza() {
        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();

        CustomException ex = assertThrows(CustomException.class,
                () -> service.paginar(request, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(asistenciaActividadRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void paginar_backoffice_devuelvePagina() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        AsistenciaActividad registro = new AsistenciaActividad();
        registro.setUuid("asistencia-uuid");
        registro.setFecha(LocalDate.of(2025, 6, 10));
        when(asistenciaActividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registro)));

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");
        request.setFechaDesde(LocalDate.of(2025, 6, 1));
        request.setFechaHasta(LocalDate.of(2025, 6, 30));

        var result = service.paginar(request, false);

        assertEquals(1, result.getTotalElements());
        assertEquals("asistencia-uuid", result.getContent().get(0).getUuid());
    }

    @Test
    void paginar_porMember_resuelveFiltro() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        Member member = new Member();
        member.setId(5L);
        when(memberAuthService.resolveMemberByUuid("member-uuid", 1L)).thenReturn(member);
        when(asistenciaActividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");
        request.setMemberUuid("member-uuid");

        service.paginar(request, false);

        verify(memberAuthService).resolveMemberByUuid("member-uuid", 1L);
    }

    @Test
    void paginar_portal_porMember_validaInscripcionYFiltra() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        Profesor profesor = new Profesor();
        profesor.setId(3L);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 3L, 0)).thenReturn(true);

        Member member = new Member();
        member.setId(5L);
        when(memberAuthService.resolveMemberByUuid("member-uuid", 1L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(10L, 5L, 0))
                .thenReturn(true);
        when(asistenciaActividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");
        request.setMemberUuid("member-uuid");

        service.paginar(request, true);

        verify(memberAuthService).resolveMemberByUuid("member-uuid", 1L);
        verify(inscripcionActividadRepository).existsByActividad_IdAndMember_IdAndStatus(10L, 5L, 0);
    }

    @Test
    void paginar_portal_memberNoInscripto_rechaza() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        Profesor profesor = new Profesor();
        profesor.setId(3L);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 3L, 0)).thenReturn(true);

        Member member = new Member();
        member.setId(5L);
        when(memberAuthService.resolveMemberByUuid("member-uuid", 1L)).thenReturn(member);
        when(inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(10L, 5L, 0))
                .thenReturn(false);

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");
        request.setMemberUuid("member-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> service.paginar(request, true));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void paginar_portal_rechazaProfesorNoAsignado() {
        Actividad actividad = new Actividad();
        actividad.setId(10L);
        when(actividadRepository.findByUuidAndCompany_IdAndStatus("actividad-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        Profesor profesor = new Profesor();
        profesor.setId(3L);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 3L, 0)).thenReturn(false);

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");

        CustomException ex = assertThrows(CustomException.class,
                () -> service.paginar(request, true));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void paginar_rangoFechasInvalido_rechaza() {
        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");
        request.setFechaDesde(LocalDate.of(2025, 6, 30));
        request.setFechaHasta(LocalDate.of(2025, 6, 1));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.paginar(request, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void paginar_sinPermiso_rechaza() {
        stubAuthService.setAllowAccess(false);

        AsistenciaPaginateRequestDTO request = new AsistenciaPaginateRequestDTO();
        request.setActividadUuid("actividad-uuid");

        assertThrows(CustomException.class, () -> service.paginar(request, false));
    }
}
