package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoActividadQueryServiceTest {

    private static final Long COMPANY_ID = 10L;

    @Mock
    private EventoActividadRepository eventoActividadRepository;

    private EventoActividadQueryService service;
    private StubAuthService stubAuthService;

    @BeforeEach
    void setUp() {
        service = new EventoActividadQueryService();
        stubAuthService = new StubAuthService();
        Company company = new Company();
        company.setId(COMPANY_ID);
        stubAuthService.setCompany(company);

        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "eventoActividadRepository", eventoActividadRepository);
        ReflectionTestUtils.setField(service, "eventoActividadMapper", new EventoActividadMapper());
    }

    @SuppressWarnings("unchecked")
    @Test
    void paginar_devuelveDtoConActividadNombre() {
        Actividad actividad = new Actividad();
        actividad.setUuid("act-uuid");
        actividad.setNombre("Futbol");

        EventoActividad evento = new EventoActividad();
        evento.setUuid("evento-uuid");
        evento.setTitulo("Torneo interno");
        evento.setDescripcion("Encuentro amistoso");
        evento.setUbicacion("Cancha 1");
        evento.setInicio(LocalDateTime.of(2026, 6, 15, 18, 0));
        evento.setFin(LocalDateTime.of(2026, 6, 15, 20, 0));
        evento.setEstado(EventoActividadEstadoEnum.CONFIRMADO);
        evento.setActividad(actividad);

        when(eventoActividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evento)));

        Page<EventoActividadDetalleDTO> result = service.paginar(new EventoActividadPaginateRequestDTO());

        assertEquals(1, result.getTotalElements());
        EventoActividadDetalleDTO dto = result.getContent().get(0);
        assertEquals("evento-uuid", dto.getUuid());
        assertEquals("Torneo interno", dto.getTitulo());
        assertEquals("act-uuid", dto.getActividadUuid());
        assertEquals("Futbol", dto.getActividadNombre());
    }

    @Test
    void paginar_requestNull_usaDefaults() {
        when(eventoActividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<EventoActividadDetalleDTO> result = service.paginar(null);

        assertEquals(0, result.getTotalElements());
    }

    private static class StubAuthService extends AuthService {

        private Company company = new Company();

        void setCompany(Company company) {
            this.company = company;
        }

        @Override
        public User valid() {
            return new User();
        }

        @Override
        public User valid(List<Enum<?>> required) {
            return valid();
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            return company;
        }
    }
}
