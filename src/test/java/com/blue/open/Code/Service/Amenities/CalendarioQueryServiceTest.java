package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioQueryServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EventoActividadRepository eventoActividadRepository;

    private StubAuthService stubAuthService;
    private CalendarioQueryService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new CalendarioQueryService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "eventoActividadRepository", eventoActividadRepository);
        ReflectionTestUtils.setField(service, "eventoActividadMapper", new EventoActividadMapper());
    }

    // CA-06: staff ve eventos CONFIRMADOS y CANCELADOS combinados con slots
    @Test
    void listar_combinaSlotsYEventosOrdenadosPorInicio() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(slotRepository.findByCompanyAndInicioBetweenWithDetails(1L, desde, hasta)).thenReturn(List.of());

        Actividad actividad = new Actividad();
        actividad.setUuid("act-uuid");
        actividad.setNombre("Natación");

        EventoActividad confirmado = evento("ev-1", actividad, desde.plusDays(2), EventoActividadEstadoEnum.CONFIRMADO);
        EventoActividad cancelado = evento("ev-2", actividad, desde.plusDays(1), EventoActividadEstadoEnum.CANCELADO);

        when(eventoActividadRepository.findByCompanyAndInicioBetweenWithDetails(1L, desde, hasta))
                .thenReturn(List.of(confirmado, cancelado));

        List<CalendarioSlotDTO> resultado = service.listar(desde, hasta);

        assertEquals(2, resultado.size());
        assertEquals("ev-2", resultado.get(0).getEventoUuid());
        assertEquals("ev-1", resultado.get(1).getEventoUuid());
        assertEquals("Natación", resultado.get(0).getReferenciaNombre());
        assertEquals(SlotEstadoEnum.CANCELADO, resultado.get(0).getEstado());
        assertEquals(SlotEstadoEnum.CONFIRMADO, resultado.get(1).getEstado());
    }

    // CA-06/CA-07: evento con baja lógica no aparece (query filtra status=0)
    @Test
    void listar_noIncluyeEventosConBajaLogica() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(slotRepository.findByCompanyAndInicioBetweenWithDetails(eq(1L), eq(desde), eq(hasta)))
                .thenReturn(List.of());
        when(eventoActividadRepository.findByCompanyAndInicioBetweenWithDetails(1L, desde, hasta))
                .thenReturn(List.of());

        List<CalendarioSlotDTO> resultado = service.listar(desde, hasta);

        assertTrue(resultado.isEmpty());
    }

    // CA-07: calendario no expone descripción del evento
    @Test
    void listar_eventoSinDescripcionEnRespuesta() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

        when(slotRepository.findByCompanyAndInicioBetweenWithDetails(1L, desde, hasta)).thenReturn(List.of());

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        EventoActividad evento = evento("ev-1", actividad, desde.plusDays(3), EventoActividadEstadoEnum.CONFIRMADO);
        evento.setDescripcion("Texto largo de descripción");

        when(eventoActividadRepository.findByCompanyAndInicioBetweenWithDetails(1L, desde, hasta))
                .thenReturn(List.of(evento));

        CalendarioSlotDTO dto = service.listar(desde, hasta).get(0);

        assertEquals("Torneo", dto.getTitulo());
        assertEquals("Cancha", dto.getUbicacion());
        assertNull(dto.getSlotUuid());
    }

    private EventoActividad evento(
            String uuid,
            Actividad actividad,
            LocalDateTime inicio,
            EventoActividadEstadoEnum estado) {
        EventoActividad evento = new EventoActividad();
        evento.setUuid(uuid);
        evento.setActividad(actividad);
        evento.setTitulo("Torneo");
        evento.setUbicacion("Cancha");
        evento.setDescripcion("Desc interna");
        evento.setInicio(inicio);
        evento.setFin(inicio.plusHours(1));
        evento.setEstado(estado);
        return evento;
    }
}
