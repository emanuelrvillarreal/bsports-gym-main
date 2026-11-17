package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.ReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Service.Amenities.ReservaAmenityAltaService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmenityControllerTest {

    @Mock
    private ReservaAmenityAltaService reservaAmenityAltaService;

    private AmenityController controller;

    @BeforeEach
    void setUp() {
        controller = new AmenityController();
        ReflectionTestUtils.setField(controller, "reservaAmenityAltaService", reservaAmenityAltaService);
    }

    // RF-04 / CA-04 · RF-05 / CA-05 · RF-07 / CA-07 / TC-01: Dado command válido, cuando crearReserva,
    // entonces delega al service y retorna 200 con ReservaAmenityDTO
    @Test
    void crearReserva_delegaAlServiceYRetorna200ConDto() {
        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("member-uuid");
        command.setInicio(LocalDateTime.of(2026, 6, 7, 14, 0));
        command.setFin(LocalDateTime.of(2026, 6, 7, 16, 0));

        ReservaAmenityDTO expected = new ReservaAmenityDTO();
        expected.setReservaUuid("reserva-uuid");
        expected.setAmenityUuid("amenity-uuid");
        expected.setMemberUuid("member-uuid");
        expected.setSlotUuid("slot-uuid");
        expected.setSlotEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);
        expected.setMontoTotal(new BigDecimal("2000.00"));
        expected.setMontoSena(new BigDecimal("600.00"));
        expected.setSaldoPendiente(new BigDecimal("1400.00"));
        expected.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);

        when(reservaAmenityAltaService.crear(same(command))).thenReturn(expected);

        ResponseEntity<ReservaAmenityDTO> response = controller.crearReserva(command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("reserva-uuid", response.getBody().getReservaUuid());
        assertEquals(new BigDecimal("2000.00"), response.getBody().getMontoTotal());
        verify(reservaAmenityAltaService).crear(command);
    }

    // RF-04 / CA-04 / TC-02: Dado familiar no titular, cuando crearReserva, entonces propaga CustomException
    @Test
    void crearReserva_propagaErrorNoTitular() {
        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("fam-uuid");
        command.setInicio(LocalDateTime.of(2026, 6, 7, 14, 0));
        command.setFin(LocalDateTime.of(2026, 6, 7, 16, 0));

        when(reservaAmenityAltaService.crear(any()))
                .thenThrow(new CustomException(HttpStatus.BAD_REQUEST,
                        "El identificador no corresponde a un socio titular",
                        "AmenityReservaValidacionService | uuid: fam-uuid"));

        CustomException ex = assertThrows(CustomException.class, () -> controller.crearReserva(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // RF-04 / CA-04 / TC-03: Dado tope semanal alcanzado, cuando crearReserva, entonces propaga CustomException
    @Test
    void crearReserva_propagaErrorTopeSemanal() {
        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("member-uuid");
        command.setInicio(LocalDateTime.of(2026, 6, 7, 14, 0));
        command.setFin(LocalDateTime.of(2026, 6, 7, 16, 0));

        when(reservaAmenityAltaService.crear(any()))
                .thenThrow(new CustomException(HttpStatus.BAD_REQUEST,
                        "El titular ya alcanzó el máximo de 2 reservas de amenities por semana",
                        "AmenityReservaValidacionService | memberId: 10"));

        CustomException ex = assertThrows(CustomException.class, () -> controller.crearReserva(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // RF-05 / CA-05 / TC-04: Dado solapamiento mismo amenity, cuando crearReserva, entonces propaga CustomException
    @Test
    void crearReserva_propagaErrorSolapamiento() {
        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("member-uuid");
        command.setInicio(LocalDateTime.of(2026, 6, 7, 14, 0));
        command.setFin(LocalDateTime.of(2026, 6, 7, 16, 0));

        when(reservaAmenityAltaService.crear(any()))
                .thenThrow(new CustomException(HttpStatus.CONFLICT,
                        "Ya existe un slot solapado para este amenity",
                        "AmenityReservaValidacionService | amenityId: 5"));

        CustomException ex = assertThrows(CustomException.class, () -> controller.crearReserva(command));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // RF-05 / CA-05 / TC-05: Dado horario fuera de disponibilidad del amenity, cuando crearReserva,
    // entonces propaga CustomException
    @Test
    void crearReserva_propagaErrorHorarioAmenity() {
        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("member-uuid");
        command.setInicio(LocalDateTime.of(2026, 6, 8, 14, 0));
        command.setFin(LocalDateTime.of(2026, 6, 8, 16, 0));

        when(reservaAmenityAltaService.crear(any()))
                .thenThrow(new CustomException(HttpStatus.BAD_REQUEST,
                        "El día del slot no está disponible para este amenity",
                        "AmenityReservaValidacionService | dia: SUNDAY"));

        CustomException ex = assertThrows(CustomException.class, () -> controller.crearReserva(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
