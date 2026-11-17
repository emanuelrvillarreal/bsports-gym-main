package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmenityBajaServiceTest {

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private StubAuthService stubAuthService;
    private AmenityBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new AmenityBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "amenityRepository", amenityRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
    }

    // RF-02 / RF-10 / CA-02 / CA-10 / TC-19 / TC-30: Dado amenity activo con reservas futuras,
    // cuando desactivar, entonces slots CANCELADOS + reservas CANCELADA + saldo 0 (camino 3)
    @Test
    void desactivar_cancelaSlotsFuturosBatchYMarcaInactivo() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Amenity amenity = new Amenity();
        amenity.setId(3L);
        amenity.setUuid("amenity-uuid");
        amenity.setStatus(0);

        Slot slotFuturo = new Slot();
        slotFuturo.setId(50L);
        slotFuturo.setEstado(SlotEstadoEnum.CONFIRMADO);

        Reserva reserva = new Reserva();
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(new BigDecimal("400.00"));

        when(amenityRepository.findByUuidAndCompanyIdAndStatusActive("amenity-uuid", 1L))
                .thenReturn(Optional.of(amenity));
        when(slotRepository.findFuturosByAmenityId(eq(3L), any(LocalDateTime.class), any()))
                .thenReturn(List.of(slotFuturo));
        when(reservaRepository.findByCompanyIdAndSlotIdIn(1L, List.of(50L)))
                .thenReturn(List.of(reserva));
        when(amenityRepository.save(amenity)).thenReturn(amenity);
        when(slotRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desactivar("amenity-uuid");

        ArgumentCaptor<List<Slot>> slotCaptor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(slotCaptor.capture());
        assertEquals(SlotEstadoEnum.CANCELADO, slotCaptor.getValue().get(0).getEstado());

        ArgumentCaptor<List<Reserva>> reservaCaptor = ArgumentCaptor.forClass(List.class);
        verify(reservaRepository).saveAll(reservaCaptor.capture());
        assertEquals(ReservaEstadoEnum.CANCELADA, reservaCaptor.getValue().get(0).getEstado());
        assertEquals(BigDecimal.ZERO, reservaCaptor.getValue().get(0).getSaldoPendiente());
        assertEquals(-1, amenity.getStatus());
    }
}
