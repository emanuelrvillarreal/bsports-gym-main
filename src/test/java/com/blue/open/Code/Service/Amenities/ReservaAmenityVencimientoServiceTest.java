package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaAmenityVencimientoServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SlotRepository slotRepository;

    private ReservaAmenityVencimientoService service;

    @BeforeEach
    void setUp() {
        service = new ReservaAmenityVencimientoService();
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
    }

    // RF-08 / RF-10 / CA-08 / CA-10 / TC-18 / TC-29: Dado reserva sin sena vencida 48 h,
    // cuando procesarVencidas, entonces VENCIDA + slot eliminado (camino 2)
    @Test
    void procesarVencidas_cancelaReservaYEliminaSlotFisicamente() {
        Slot slot = new Slot();
        slot.setId(99L);

        Reserva reserva = new Reserva();
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setVenceSenaAt(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
        reserva.setSlot(slot);

        when(reservaRepository.findPendientesSenaVencidas(eq(ReservaEstadoEnum.PENDIENTE_SENA), any()))
                .thenReturn(List.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        var result = service.procesarVencidas();

        assertEquals(1, result.getReservasVencidas());
        assertEquals(1, result.getSlotsEliminados());
        assertEquals(ReservaEstadoEnum.VENCIDA, reserva.getEstado());
        verify(slotRepository).delete(slot);
    }
}
