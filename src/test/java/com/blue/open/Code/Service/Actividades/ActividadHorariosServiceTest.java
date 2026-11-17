package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadHorarioCommand;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Repository.ActividadHorarioRepository;
import com.blue.open.Code.Repository.ActividadRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadHorariosServiceTest {

    @Mock
    private ActividadHorarioRepository actividadHorarioRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private ActividadHorariosService service;
    private Actividad actividad;
    private ActividadHorario lunes;
    private ActividadHorario miercoles;

    @BeforeEach
    void setUp() {
        service = new ActividadHorariosService();
        ReflectionTestUtils.setField(service, "actividadHorarioRepository", actividadHorarioRepository);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);

        Company company = new Company();
        company.setId(1L);

        actividad = new Actividad();
        actividad.setId(5L);
        actividad.setCompany(company);

        lunes = horario(7L, "hor-lunes", DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0), 2);
        miercoles = horario(8L, "hor-miercoles", DiaSemanaEnum.MIERCOLES, LocalTime.of(12, 0), LocalTime.of(13, 30), 2);
    }

    // ---- sincronizar ----

    @Test
    void sincronizar_listaVacia_lanzaBadRequest() {
        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, List.of()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("La actividad debe tener al menos un horario", ex.getUserMessage());
        verify(actividadHorarioRepository, never()).saveAll(any());
    }

    @Test
    void sincronizar_listaNula_lanzaBadRequest() {
        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void sincronizar_mismoDiaYHoraRepetidos_lanzaBadRequest() {
        List<ActividadHorarioCommand> commands = List.of(
                command(null, DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0), 10),
                command(null, DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(20, 0), 5));

        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, commands));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Hay dos horarios el lunes a las 18:00", ex.getUserMessage());
        verify(actividadHorarioRepository, never()).saveAll(any());
    }

    @Test
    void sincronizar_quitarHorarioConInscriptos_lanzaBadRequest() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{8L, 1L}));

        // Se conserva solo el lunes: el miercoles tiene un inscripto.
        List<ActividadHorarioCommand> commands = List.of(
                command("hor-lunes", DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0), 2));

        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, commands));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getUserMessage().startsWith("No se puede quitar el horario del miércoles de 12:00 a 13:30"));
        assertEquals(0, miercoles.getStatus());
        verify(actividadHorarioRepository, never()).saveAll(any());
    }

    @Test
    void sincronizar_cupoMenorAInscriptos_lanzaBadRequest() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}));

        List<ActividadHorarioCommand> commands = List.of(
                command("hor-lunes", DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0), 1));

        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, commands));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El cupo de el horario del lunes de 18:00 a 19:00 no puede ser menor a sus 2 inscriptos",
                ex.getUserMessage());
        assertEquals(2, lunes.getCupoMaximo());
        verify(actividadHorarioRepository, never()).saveAll(any());
    }

    @Test
    void sincronizar_horarioDeOtraActividad_lanzaBadRequest() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes));

        List<ActividadHorarioCommand> commands = List.of(
                command("hor-ajeno", DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0), 5));

        CustomException ex = assertThrows(CustomException.class, () -> service.sincronizar(actividad, commands));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El horario no pertenece a la actividad", ex.getUserMessage());
    }

    @Test
    void sincronizar_actualizaCreaYBajaSinInscriptos_yRecalculaResumen() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}));

        List<ActividadHorarioCommand> commands = List.of(
                command("hor-lunes", DiaSemanaEnum.LUNES, LocalTime.of(17, 0), LocalTime.of(19, 0), 4),
                command(null, DiaSemanaEnum.VIERNES, LocalTime.of(20, 0), LocalTime.of(21, 30), 6));

        List<ActividadHorario> resultado = service.sincronizar(actividad, commands);

        assertEquals(2, resultado.size());
        assertEquals(lunes, resultado.get(0));
        assertEquals(LocalTime.of(17, 0), lunes.getHoraInicio());
        assertEquals(4, lunes.getCupoMaximo());
        ActividadHorario nuevo = resultado.get(1);
        assertEquals(DiaSemanaEnum.VIERNES, nuevo.getDiaSemana());
        assertEquals(actividad, nuevo.getActividad());
        assertEquals(actividad.getCompany(), nuevo.getCompany());
        // El miercoles no se envio y no tenia inscriptos: se da de baja.
        assertEquals(1, miercoles.getStatus());
        verify(actividadHorarioRepository).save(miercoles);
        verify(actividadHorarioRepository).saveAll(resultado);
        // Resumen de la actividad a partir de los horarios vigentes.
        assertEquals(10, actividad.getCupoMaximo());
        assertEquals(LocalTime.of(17, 0), actividad.getHoraInicio());
        assertEquals(LocalTime.of(21, 30), actividad.getHoraFin());
        verify(actividadRepository).save(actividad);
    }

    // ---- resolverEleccion ----

    @Test
    void resolverEleccion_sinHorarios_lanzaBadRequest() {
        CustomException ex = assertThrows(CustomException.class,
                () -> service.resolverEleccion(actividad, List.of(), null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Elegí al menos un horario", ex.getUserMessage());
    }

    @Test
    void resolverEleccion_horarioLleno_lanzaBadRequest() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.resolverEleccion(actividad, List.of("hor-lunes"), null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("No quedan lugares en el horario del lunes de 18:00 a 19:00", ex.getUserMessage());
    }

    @Test
    void resolverEleccion_horarioLlenoQueYaOcupaElSocio_loPermite() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}, new Object[]{8L, 0L}));
        InscripcionActividad actual = new InscripcionActividad();
        actual.getHorarios().add(lunes);

        Set<ActividadHorario> elegidos = assertDoesNotThrow(() ->
                service.resolverEleccion(actividad, List.of("hor-lunes", "hor-miercoles"), actual));

        assertEquals(Set.of(lunes, miercoles), elegidos);
    }

    @Test
    void resolverEleccion_uuidsRepetidos_devuelveSinRepetidos() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any())).thenReturn(List.of());

        Set<ActividadHorario> elegidos = service.resolverEleccion(actividad, List.of("hor-lunes", "hor-lunes"), null);

        assertEquals(Set.of(lunes), elegidos);
    }

    // ---- sincronizarCompleta ----

    @Test
    void sincronizarCompleta_todosLosHorariosLlenos_marcaCompleta() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}, new Object[]{8L, 2L}));

        service.sincronizarCompleta(actividad);

        assertTrue(actividad.isCompleta());
        verify(actividadRepository).save(actividad);
    }

    @Test
    void sincronizarCompleta_algunHorarioConLugar_noMarcaCompleta() {
        when(actividadHorarioRepository.findActivosByActividadId(5L)).thenReturn(List.of(lunes, miercoles));
        when(actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(any()))
                .thenReturn(filas(new Object[]{7L, 2L}, new Object[]{8L, 1L}));

        service.sincronizarCompleta(actividad);

        assertFalse(actividad.isCompleta());
        verify(actividadRepository, never()).save(any());
    }

    // ---- validarDiaVencimientoFijo ----

    @Test
    void validarDiaVencimientoFijo_conCuotaSemanal_lanzaBadRequest() {
        actividad.setPeriodicidadCuota(PeriodicidadCuotaEnum.SEMANAL);
        actividad.setDiaVencimientoFijo(10);

        CustomException ex = assertThrows(CustomException.class,
                () -> ActividadHorariosService.validarDiaVencimientoFijo(actividad));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarDiaVencimientoFijo_conCuotaMensual_esValido() {
        actividad.setPeriodicidadCuota(PeriodicidadCuotaEnum.MENSUAL);
        actividad.setDiaVencimientoFijo(10);

        assertDoesNotThrow(() -> ActividadHorariosService.validarDiaVencimientoFijo(actividad));
    }

    private ActividadHorario horario(Long id, String uuid, DiaSemanaEnum dia, LocalTime inicio, LocalTime fin, int cupo) {
        ActividadHorario horario = new ActividadHorario();
        horario.setId(id);
        horario.setUuid(uuid);
        horario.setActividad(actividad);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setCupoMaximo(cupo);
        return horario;
    }

    private static ActividadHorarioCommand command(
            String uuid, DiaSemanaEnum dia, LocalTime inicio, LocalTime fin, int cupo) {
        ActividadHorarioCommand command = new ActividadHorarioCommand();
        command.setUuid(uuid);
        command.setDiaSemana(dia);
        command.setHoraInicio(inicio);
        command.setHoraFin(fin);
        command.setCupoMaximo(cupo);
        return command;
    }

    private static List<Object[]> filas(Object[]... filas) {
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            resultado.add(fila);
        }
        return resultado;
    }
}
