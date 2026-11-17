package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Repository.ActividadHorarioRepository;
import com.blue.open.Code.Repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotGeneracionServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ActividadHorarioRepository actividadHorarioRepository;

    @InjectMocks
    private SlotGeneracionService service;

    private Actividad actividad;
    private ActividadHorario lunes;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        actividad = new Actividad();
        actividad.setId(10L);
        actividad.setStatus(0);
        actividad.setCompany(company);
        actividad.setDiasSemana("LUNES");
        actividad.setHoraInicio(LocalTime.of(18, 0));
        actividad.setHoraFin(LocalTime.of(19, 30));
        actividad.setImporteCuota(BigDecimal.TEN);
        actividad.setCupoMaximo(20);

        lunes = horario(100L, DiaSemanaEnum.LUNES, LocalTime.of(18, 0), LocalTime.of(19, 30));

        lenient().when(actividadHorarioRepository.findActivosByActividadId(10L)).thenReturn(List.of(lunes));
        lenient().when(slotRepository.findIniciosByHorarioIdAndInicioBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void generarVentanaCompleta_creaUnSlotPorCadaLunesEnVentana() {
        LocalDate desde = LocalDate.of(2026, 6, 2);

        int creados = service.generarVentanaCompleta(actividad, desde);

        assertEquals(13, creados);
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());
        assertEquals(13, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(slot -> slot.getHorario() == lunes));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void generarVentanaCompleta_iteraPorCadaHorarioConfigurado() {
        ActividadHorario miercoles = horario(101L, DiaSemanaEnum.MIERCOLES, LocalTime.of(12, 0), LocalTime.of(13, 30));
        when(actividadHorarioRepository.findActivosByActividadId(10L)).thenReturn(List.of(lunes, miercoles));
        LocalDate desde = LocalDate.of(2026, 6, 2);

        int creados = service.generarVentanaCompleta(actividad, desde);

        assertEquals(27, creados);
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository, times(2)).saveAll(captor.capture());
        List<Slot> deLunes = captor.getAllValues().get(0);
        List<Slot> deMiercoles = captor.getAllValues().get(1);
        assertEquals(13, deLunes.size());
        assertEquals(14, deMiercoles.size());
        // Cada slot toma la franja de su propio horario.
        assertSame(miercoles, deMiercoles.get(0).getHorario());
        assertEquals(LocalTime.of(12, 0), deMiercoles.get(0).getInicio().toLocalTime());
        assertEquals(LocalTime.of(13, 30), deMiercoles.get(0).getFin().toLocalTime());
    }

    @Test
    void generarVentanaCompleta_omiteIniciosYaExistentes() {
        LocalDate desde = LocalDate.of(2026, 6, 2);
        LocalDateTime inicioExistente = LocalDate.of(2026, 6, 8).atTime(18, 0);
        when(slotRepository.findIniciosByHorarioIdAndInicioBetween(eq(100L), any(), any()))
                .thenReturn(List.of(inicioExistente));

        int creados = service.generarVentanaCompleta(actividad, desde);

        assertEquals(12, creados);
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());
        assertEquals(12, captor.getValue().size());
    }

    @Test
    void regenerarTrasCambioHorarioODias_borraFuturosSinAsistenciaYRegenera() {
        LocalDateTime antes = LocalDateTime.now();

        service.regenerarTrasCambioHorarioODias(actividad);

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(slotRepository).deleteFuturosSinAsistenciaByActividadId(eq(10L), desdeCaptor.capture());
        assertFalse(desdeCaptor.getValue().isBefore(antes));
        assertFalse(desdeCaptor.getValue().isAfter(LocalDateTime.now()));
        verify(slotRepository).saveAll(any());
    }

    @Test
    void generarDiaExtension_noCreaSiDiaNoCorresponde() {
        LocalDate sabado = LocalDate.of(2026, 6, 6);
        boolean creado = service.generarDiaExtension(actividad, sabado);
        assertFalse(creado);
        verify(slotRepository, never()).saveAll(any());
    }

    @Test
    void generarDiaExtension_creaSlotDelHorarioDeEseDia() {
        LocalDate lunesObjetivo = LocalDate.of(2026, 6, 8);

        boolean creado = service.generarDiaExtension(actividad, lunesObjetivo);

        assertTrue(creado);
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(lunesObjetivo.atTime(18, 0), captor.getValue().get(0).getInicio());
        assertSame(lunes, captor.getValue().get(0).getHorario());
    }

    @Test
    void generarDiaExtension_idempotenteSiYaExiste() {
        LocalDate lunesObjetivo = LocalDate.of(2026, 6, 8);
        LocalDateTime inicio = lunesObjetivo.atTime(18, 0);
        when(slotRepository.findIniciosByHorarioIdAndInicioBetween(eq(100L), any(), any()))
                .thenReturn(List.of(inicio));

        boolean creado = service.generarDiaExtension(actividad, lunesObjetivo);

        assertFalse(creado);
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    private ActividadHorario horario(Long id, DiaSemanaEnum dia, LocalTime inicio, LocalTime fin) {
        ActividadHorario horario = new ActividadHorario();
        horario.setId(id);
        horario.setUuid("hor-" + id);
        horario.setActividad(actividad);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setCupoMaximo(20);
        return horario;
    }
}
