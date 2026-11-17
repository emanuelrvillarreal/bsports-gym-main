package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaAmenityCancelacionServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SlotRepository slotRepository;

    private StubAuthService stubAuthService;
    private ReservaAmenityCancelacionService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new ReservaAmenityCancelacionService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "amenityMapper", new AmenityMapper());
    }

    // RF-09 / RF-10 / CA-09 / CA-10 / TC-20: Dado reserva PENDIENTE_SENA futura, cuando cancelar,
    // entonces CANCELADA + slot CANCELADO + auditoría (camino 1 manual)
    @Test
    void cancelar_pendienteSenaFutura_cancelaSlotReservaYAuditoria() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        User staff = new User();
        staff.setUuid("staff-uuid");
        stubAuthService.setUser(staff);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.PENDIENTE_DE_PAGO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.PENDIENTE_SENA, slot, new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        service.cancelar("reserva-uuid");

        assertEquals(ReservaEstadoEnum.CANCELADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        assertEquals(SlotEstadoEnum.CANCELADO, slot.getEstado());
        assertEquals(staff, reserva.getCanceladaPor());
        assertNotNull(reserva.getCanceladaAt());
        verify(slotRepository).save(slot);
    }

    // RF-09 / CA-09 / TC-21: Dado reserva CONFIRMADA con saldo pendiente, cuando cancelar,
    // entonces cancela sin reversión tesorería
    @Test
    void cancelar_confirmadaConSaldo_cancelaSinReversionTesoreria() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.CONFIRMADO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.CONFIRMADA, slot, new BigDecimal("500.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        service.cancelar("reserva-uuid");

        assertEquals(ReservaEstadoEnum.CANCELADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        verify(slotRepository).save(slot);
    }

    // RF-09 / CA-09 / TC-22: Dado reserva CONFIRMADA con saldo 0 (100% cobrado), cuando cancelar,
    // entonces cancela sin reversión tesorería
    @Test
    void cancelar_confirmadaSaldoCero_cancelaSinReversionTesoreria() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.CONFIRMADO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.CONFIRMADA, slot, BigDecimal.ZERO);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        service.cancelar("reserva-uuid");

        assertEquals(ReservaEstadoEnum.CANCELADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        verify(slotRepository).save(slot);
        verify(reservaRepository).save(reserva);
    }

    // RF-09 / CA-09 / TC-23: Dado reserva VENCIDA, cuando cancelar, entonces BAD_REQUEST
    @Test
    void cancelar_vencida_rechaza() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.PENDIENTE_DE_PAGO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.VENCIDA, slot, new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        CustomException ex = assertThrows(CustomException.class, () -> service.cancelar("reserva-uuid"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(slotRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    // RF-09 / CA-09 / TC-24: Dado horario de uso pasado, cuando cancelar, entonces BAD_REQUEST
    @Test
    void cancelar_horarioPasado_rechaza() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = new Slot();
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slot.setInicio(LocalDateTime.now().minusDays(2));
        slot.setFin(LocalDateTime.now().minusDays(1));

        Reserva reserva = buildReserva(ReservaEstadoEnum.CONFIRMADA, slot, new BigDecimal("200.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        CustomException ex = assertThrows(CustomException.class, () -> service.cancelar("reserva-uuid"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(slotRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    // RF-09 / CA-09 / TC-25: Dado reserva ya CANCELADA, cuando cancelar, entonces CONFLICT
    @Test
    void cancelar_yaCancelada_conflict() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.CANCELADO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.CANCELADA, slot, BigDecimal.ZERO);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        CustomException ex = assertThrows(CustomException.class, () -> service.cancelar("reserva-uuid"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // RF-09 / CA-09 / TC-26: Dado reserva activa con slot ya CANCELADO, cuando cancelar, entonces idempotente
    @Test
    void cancelar_slotYaCancelado_alineaReservaSinReCancelarSlot() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        User staff = new User();
        staff.setUuid("staff-uuid");
        stubAuthService.setUser(staff);

        Slot slot = buildSlotFuturo(SlotEstadoEnum.CANCELADO);
        Reserva reserva = buildReserva(ReservaEstadoEnum.CONFIRMADA, slot, new BigDecimal("300.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        service.cancelar("reserva-uuid");

        assertEquals(ReservaEstadoEnum.CANCELADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        verify(slotRepository, never()).save(any());
    }

    // RF-09 / CA-09 / TC-27: Dado reserva de otro club, cuando cancelar, entonces 404
    @Test
    void cancelar_otroClub_notFound() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.cancelar("reserva-uuid"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private Reserva buildReserva(ReservaEstadoEnum estado, Slot slot, BigDecimal saldoPendiente) {
        Amenity amenity = new Amenity();
        amenity.setNombre("Parrilla");

        Member member = new Member();
        member.setId(5L);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(estado);
        reserva.setMontoTotal(new BigDecimal("1000.00"));
        reserva.setMontoSena(new BigDecimal("300.00"));
        reserva.setSaldoPendiente(saldoPendiente);
        reserva.setAmenity(amenity);
        reserva.setMember(member);
        reserva.setSlot(slot);
        return reserva;
    }

    private Slot buildSlotFuturo(SlotEstadoEnum estado) {
        Slot slot = new Slot();
        slot.setEstado(estado);
        slot.setInicio(LocalDateTime.now().plusDays(3));
        slot.setFin(LocalDateTime.now().plusDays(3).plusHours(2));
        return slot;
    }
}
