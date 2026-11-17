package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaAmenityConfirmacionServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SlotRepository slotRepository;

    private StubAuthService stubAuthService;
    private ReservaAmenityConfirmacionService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new ReservaAmenityConfirmacionService(
                stubAuthService, reservaRepository, slotRepository, new AmenityMapper());
    }

    @Test
    void confirmar_reservaGratuita_confirmaSinMovimiento() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = new Slot();
        slot.setEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);

        Amenity amenity = new Amenity();
        amenity.setNombre("Parrilla gratis");
        amenity.setMontoHora(BigDecimal.ZERO);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setMontoTotal(BigDecimal.ZERO);
        reserva.setMontoSena(BigDecimal.ZERO);
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(slotRepository.save(slot)).thenReturn(slot);

        var result = service.confirmar("reserva-uuid");

        assertEquals(ReservaEstadoEnum.CONFIRMADA, reserva.getEstado());
        assertEquals(SlotEstadoEnum.CONFIRMADO, slot.getEstado());
        assertNotNull(reserva.getSenaPagadaAt());
        assertEquals("reserva-uuid", result.getReservaUuid());
        verify(reservaRepository).save(reserva);
        verify(slotRepository).save(slot);
    }

    @Test
    void confirmar_reservaConTarifa_rechaza() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setMontoTotal(new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        assertThrows(CustomException.class, () -> service.confirmar("reserva-uuid"));
    }
}
