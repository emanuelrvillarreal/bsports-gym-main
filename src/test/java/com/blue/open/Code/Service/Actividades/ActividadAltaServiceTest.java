package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;
import com.blue.open.Code.Entity.DTO.ActividadHorarioCommand;
import com.blue.open.Code.Entity.DTO.ActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Code.Service.support.StubSlotGeneracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadAltaServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ActividadProfesorAsignacionService actividadProfesorAsignacionService;

    private final StubSlotGeneracionService slotGeneracionService = new StubSlotGeneracionService();

    @Mock
    private ActividadCategoriaResolver actividadCategoriaResolver;

    @Mock
    private ActividadHorariosService actividadHorariosService;

    private StubAuthService stubAuthService;
    private ActividadMapper actividadMapper;
    private ActividadAltaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        actividadMapper = new ActividadMapper();
        ReflectionTestUtils.setField(actividadMapper, "profesorMapper", new com.blue.open.Code.Mapper.ProfesorMapper());
        ReflectionTestUtils.setField(actividadMapper, "categoriaMapper", new com.blue.open.Code.Mapper.CategoriaMapper());
        ReflectionTestUtils.setField(actividadMapper, "actividadHorariosService", actividadHorariosService);

        service = new ActividadAltaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorAsignacionService", actividadProfesorAsignacionService);
        ReflectionTestUtils.setField(service, "slotGeneracionService", slotGeneracionService);
        ReflectionTestUtils.setField(service, "actividadMapper", actividadMapper);
        ReflectionTestUtils.setField(service, "actividadCategoriaResolver", actividadCategoriaResolver);
        ReflectionTestUtils.setField(service, "actividadHorariosService", actividadHorariosService);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void alta_conCategoriaUuid_asignaCategoria() {
        ActividadAltaCommand command = buildCommand();
        command.setCategoriaUuids(List.of("cat-uuid"));

        Categoria categoria = new Categoria();
        categoria.setUuid("cat-uuid");
        categoria.setNombre("Sub-23");

        when(actividadCategoriaResolver.resolve(1L, List.of("cat-uuid")))
                .thenReturn(new LinkedHashSet<>(List.of(categoria)));
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            a.setId(5L);
            a.setUuid("act-uuid");
            return a;
        });

        ActividadDTO dto = service.alta(command);

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(captor.capture());
        assertEquals(Set.of(categoria), captor.getValue().getCategorias());
        assertEquals(1, dto.getCategorias().size());
        assertEquals("cat-uuid", dto.getCategorias().get(0).getCategoriaUuid());
        assertEquals("Sub-23", dto.getCategorias().get(0).getNombre());
    }

    @Test
    void alta_sinCategoriaUuids_sinCategorias() {
        ActividadAltaCommand command = buildCommand();

        when(actividadCategoriaResolver.resolve(1L, null)).thenReturn(new LinkedHashSet<>());
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            a.setId(5L);
            a.setUuid("act-uuid");
            return a;
        });

        ActividadDTO dto = service.alta(command);

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(captor.capture());
        assertTrue(captor.getValue().getCategorias().isEmpty());
        assertTrue(dto.getCategorias().isEmpty());
    }

    @Test
    void alta_sincronizaHorariosDeLaActividadGuardada() {
        ActividadAltaCommand command = buildCommand();

        when(actividadCategoriaResolver.resolve(1L, null)).thenReturn(new LinkedHashSet<>());
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            a.setId(5L);
            a.setUuid("act-uuid");
            return a;
        });

        service.alta(command);

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadHorariosService).prepararResumen(captor.capture(), eq(command.getHorarios()));
        verify(actividadHorariosService).sincronizar(captor.getValue(), command.getHorarios());
        assertEquals(5L, captor.getValue().getId());
    }

    @Test
    void alta_diaVencimientoFijoConCuotaSemanal_lanzaBadRequestSinGuardar() {
        ActividadAltaCommand command = buildCommand();
        command.setPeriodicidadCuota(PeriodicidadCuotaEnum.SEMANAL);
        command.setDiaVencimientoFijo(10);

        CustomException ex = assertThrows(CustomException.class, () -> service.alta(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(actividadRepository, never()).save(any());
    }

    private ActividadAltaCommand buildCommand() {
        ActividadAltaCommand command = new ActividadAltaCommand();
        command.setNombre("Fútbol");
        command.setUbicacion("Cancha 1");
        command.setHorarios(List.of(
                horario(DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(20, 0), 20),
                horario(DiaSemanaEnum.MIERCOLES, LocalTime.of(18, 0), LocalTime.of(20, 0), 20)));
        command.setImporteCuota(new BigDecimal("5000.00"));
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
