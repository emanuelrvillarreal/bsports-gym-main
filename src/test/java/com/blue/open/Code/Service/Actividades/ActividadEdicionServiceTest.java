package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Service.support.StubSlotGeneracionService;
import org.mockito.InOrder;
import com.blue.open.Code.Entity.DTO.ActividadHorarioCommand;
import com.blue.open.Code.Entity.DTO.ActividadEdicionCommand;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadEdicionServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ActividadCategoriaResolver actividadCategoriaResolver;

    @Mock
    private ActividadProfesorAsignacionService actividadProfesorAsignacionService;

    @Mock
    private ActividadHorariosService actividadHorariosService;

    private StubSlotGeneracionService slotGeneracionService;

    private StubAuthService stubAuthService;
    private ActividadMapper actividadMapper;
    private ActividadEdicionService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        actividadMapper = new ActividadMapper();
        ReflectionTestUtils.setField(actividadMapper, "profesorMapper", new com.blue.open.Code.Mapper.ProfesorMapper());
        ReflectionTestUtils.setField(actividadMapper, "categoriaMapper", new com.blue.open.Code.Mapper.CategoriaMapper());
        ReflectionTestUtils.setField(actividadMapper, "actividadHorariosService", actividadHorariosService);
        slotGeneracionService = new StubSlotGeneracionService();

        service = new ActividadEdicionService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(service, "actividadCategoriaResolver", actividadCategoriaResolver);
        ReflectionTestUtils.setField(service, "actividadProfesorAsignacionService", actividadProfesorAsignacionService);
        ReflectionTestUtils.setField(service, "actividadMapper", actividadMapper);
        ReflectionTestUtils.setField(service, "actividadHorariosService", actividadHorariosService);
        ReflectionTestUtils.setField(service, "slotGeneracionService", slotGeneracionService);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void editar_asignaCategoria() {
        Actividad actividad = buildActividad();
        Categoria categoria = new Categoria();
        categoria.setUuid("cat-nueva");
        categoria.setNombre("Sub-25");

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadCategoriaResolver.resolve(1L, List.of("cat-nueva")))
                .thenReturn(new LinkedHashSet<>(List.of(categoria)));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());

        service.editar("act-uuid", buildCommand(List.of("cat-nueva")));

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(captor.capture());
        assertEquals(Set.of(categoria), captor.getValue().getCategorias());
    }

    @Test
    void editar_categoriaUuidsNull_conservaCategorias() {
        Actividad actividad = buildActividad();
        Categoria categoriaActual = new Categoria();
        actividad.getCategorias().add(categoriaActual);

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());

        service.editar("act-uuid", buildCommand(null));

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(captor.capture());
        assertEquals(Set.of(categoriaActual), captor.getValue().getCategorias());
    }

    @Test
    void editar_actividadInexistente_lanzaNotFound() {
        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("missing", 1L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.editar("missing", buildCommand(null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void editar_profesorUuidsNull_noModificaAsignaciones() {
        Actividad actividad = buildActividad();

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());

        ActividadEdicionCommand command = buildCommand(null);
        command.setProfesorUuids(null);

        service.editar("act-uuid", command);

        verifyNoInteractions(actividadProfesorAsignacionService);
    }

    @Test
    void editar_reasignaProfesores_agregaYQuita() {
        Actividad actividad = buildActividad();

        Profesor profesorA = buildProfesor(10L, "prof-a", ProfesorEstadoEnum.ACTIVO);
        Profesor profesorB = buildProfesor(11L, "prof-b", ProfesorEstadoEnum.ACTIVO);

        ActividadProfesor linkA = new ActividadProfesor();
        linkA.setActividadId(5L);
        linkA.setProfesorId(10L);

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of(linkA));
        when(profesorRepository.findByIdInWithUser(List.of(10L))).thenReturn(List.of(profesorA));
        when(profesorRepository.findByUuidAndCompanyIdWithUser("prof-b", 1L)).thenReturn(Optional.of(profesorB));

        ActividadEdicionCommand command = buildCommand(null);
        command.setProfesorUuids(List.of("prof-b"));

        service.editar("act-uuid", command);

        verify(actividadProfesorAsignacionService).desasignar(actividad, profesorA);
        verify(actividadProfesorAsignacionService).asignar(actividad, profesorB);
    }

    @Test
    void editar_profesorUuidsVacio_desasignaATodos() {
        Actividad actividad = buildActividad();

        Profesor profesorA = buildProfesor(10L, "prof-a", ProfesorEstadoEnum.ACTIVO);

        ActividadProfesor linkA = new ActividadProfesor();
        linkA.setActividadId(5L);
        linkA.setProfesorId(10L);

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of(linkA));
        when(profesorRepository.findByIdInWithUser(List.of(10L))).thenReturn(List.of(profesorA));

        ActividadEdicionCommand command = buildCommand(null);
        command.setProfesorUuids(List.of());

        service.editar("act-uuid", command);

        verify(actividadProfesorAsignacionService).desasignar(actividad, profesorA);
        verify(actividadProfesorAsignacionService, never()).asignar(any(), any());
    }

    @Test
    void editar_profesorInexistente_lanzaBadRequest() {
        Actividad actividad = buildActividad();

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());
        when(profesorRepository.findByUuidAndCompanyIdWithUser("prof-x", 1L)).thenReturn(Optional.empty());

        ActividadEdicionCommand command = buildCommand(null);
        command.setProfesorUuids(List.of("prof-x"));

        CustomException ex = assertThrows(CustomException.class, () -> service.editar("act-uuid", command));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void editar_profesorInactivo_lanzaBadRequest() {
        Actividad actividad = buildActividad();
        Profesor profesorInactivo = buildProfesor(12L, "prof-inactivo", ProfesorEstadoEnum.INACTIVO);

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());
        when(profesorRepository.findByUuidAndCompanyIdWithUser("prof-inactivo", 1L))
                .thenReturn(Optional.of(profesorInactivo));

        ActividadEdicionCommand command = buildCommand(null);
        command.setProfesorUuids(List.of("prof-inactivo"));

        CustomException ex = assertThrows(CustomException.class, () -> service.editar("act-uuid", command));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void editar_sincronizaHorariosYRegeneraClases() {
        Actividad actividad = buildActividad();

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(5L))).thenReturn(List.of());

        ActividadEdicionCommand command = buildCommand(null);
        service.editar("act-uuid", command);

        InOrder orden = inOrder(actividadHorariosService);
        orden.verify(actividadHorariosService).sincronizar(actividad, command.getHorarios());
        orden.verify(actividadHorariosService).sincronizarCompleta(actividad);
        assertEquals(1, slotGeneracionService.getRegeneraciones());
        assertSame(actividad, slotGeneracionService.getUltimaActividadRegenerada());
    }

    @Test
    void editar_horarioConInscriptosQuitado_propagaErrorSinRegenerar() {
        Actividad actividad = buildActividad();

        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(actividad)).thenReturn(actividad);
        ActividadEdicionCommand command = buildCommand(null);
        when(actividadHorariosService.sincronizar(actividad, command.getHorarios()))
                .thenThrow(new CustomException(HttpStatus.BAD_REQUEST, "No se puede quitar el horario", "test"));

        CustomException ex = assertThrows(CustomException.class, () -> service.editar("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(0, slotGeneracionService.getRegeneraciones());
    }

    @Test
    void editar_diaVencimientoFijoConCuotaSemanal_lanzaBadRequestSinGuardar() {
        Actividad actividad = buildActividad();
        when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", 1L))
                .thenReturn(Optional.of(actividad));

        ActividadEdicionCommand command = buildCommand(null);
        command.setPeriodicidadCuota(PeriodicidadCuotaEnum.SEMANAL);
        command.setDiaVencimientoFijo(10);

        CustomException ex = assertThrows(CustomException.class, () -> service.editar("act-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(actividadRepository, never()).save(any());
    }

    private Profesor buildProfesor(Long id, String uuid, ProfesorEstadoEnum estado) {
        Profesor profesor = new Profesor();
        profesor.setId(id);
        profesor.setUuid(uuid);
        profesor.setEstado(estado);
        User user = new User();
        user.setNombre("Profesor " + uuid);
        profesor.setUser(user);
        return profesor;
    }

    private Actividad buildActividad() {
        Actividad actividad = new Actividad();
        actividad.setId(5L);
        actividad.setUuid("act-uuid");
        actividad.setNombre("Fútbol");
        actividad.setUbicacion("Cancha 1");
        actividad.setCupoMaximo(20);
        actividad.setDiasSemana("LUNES");
        actividad.setHoraInicio(LocalTime.of(18, 0));
        actividad.setHoraFin(LocalTime.of(20, 0));
        actividad.setImporteCuota(new BigDecimal("5000.00"));
        return actividad;
    }

    private ActividadEdicionCommand buildCommand(List<String> categoriaUuids) {
        ActividadEdicionCommand command = new ActividadEdicionCommand();
        command.setNombre("Fútbol actualizado");
        command.setUbicacion("Cancha 2");
        command.setHorarios(List.of(
                horario(DiaSemanaEnum.MARTES, LocalTime.of(17, 0), LocalTime.of(19, 0), 25)));
        command.setImporteCuota(new BigDecimal("6000.00"));
        command.setCategoriaUuids(categoriaUuids);
        return command;
    }

    private static ActividadHorarioCommand horario(DiaSemanaEnum dia, LocalTime inicio, LocalTime fin, int cupo) {
        ActividadHorarioCommand horario = new ActividadHorarioCommand();
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setCupoMaximo(cupo);
        return horario;
    }
}
