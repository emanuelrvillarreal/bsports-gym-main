package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.ReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaAmenityAltaServiceTest {

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AmenityReservaValidacionService validacionService;

    private StubAuthService stubAuthService;
    private ReservaAmenityAltaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new ReservaAmenityAltaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "amenityRepository", amenityRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "validacionService", validacionService);
        ReflectionTestUtils.setField(service, "amenityMapper", new AmenityMapper());
    }

    // RF-04/05/07/08 / CA-04·05·07·08 / TC-16: Dado alta válida, cuando crear, entonces slot PENDIENTE_DE_PAGO y montos
    @Test
    void crear_persisteSlotPendienteDePagoYReservaConMontos() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Amenity amenity = new Amenity();
        amenity.setId(5L);
        amenity.setUuid("amenity-uuid");
        amenity.setNombre("Parrilla");
        amenity.setMontoHora(new BigDecimal("1000.00"));
        amenity.setPorcentajeSena(new BigDecimal("30.00"));

        Member titular = new Member();
        titular.setId(10L);

        LocalDate slotDate = LocalDate.now().plusDays(3);
        while (slotDate.getDayOfWeek().getValue() != 6) {
            slotDate = slotDate.plusDays(1);
        }
        LocalDateTime inicio = slotDate.atTime(14, 0);
        LocalDateTime fin = inicio.plusMinutes(45);

        when(amenityRepository.findByUuidAndCompanyIdAndStatusActive("amenity-uuid", 1L))
                .thenReturn(Optional.of(amenity));
        when(validacionService.validarTitular("member-uuid", 1L)).thenReturn(titular);
        doNothing().when(validacionService).validarTopeSemanal(1L, 10L, inicio);
        doNothing().when(validacionService).validarHorarioAmenity(amenity, inicio, fin);
        doNothing().when(validacionService).validarSolapamiento(5L, inicio, fin);

        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> {
            Slot s = inv.getArgument(0);
            s.setId(100L);
            s.setUuid("slot-uuid");
            return s;
        });
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setUuid("reserva-uuid");
            return r;
        });

        ReservaAmenityAltaCommand command = new ReservaAmenityAltaCommand();
        command.setAmenityUuid("amenity-uuid");
        command.setMemberUuid("member-uuid");
        command.setInicio(inicio);
        command.setFin(fin);

        service.crear(command);

        org.mockito.ArgumentCaptor<Slot> slotCaptor = org.mockito.ArgumentCaptor.forClass(Slot.class);
        verify(slotRepository).save(slotCaptor.capture());
        assertEquals(SlotEstadoEnum.PENDIENTE_DE_PAGO, slotCaptor.getValue().getEstado());

        org.mockito.ArgumentCaptor<Reserva> reservaCaptor = org.mockito.ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reservaCaptor.capture());
        Reserva saved = reservaCaptor.getValue();
        assertEquals(ReservaEstadoEnum.PENDIENTE_SENA, saved.getEstado());
        assertEquals(new BigDecimal("1000.00"), saved.getMontoTotal());
        assertEquals(new BigDecimal("300.00"), saved.getMontoSena());
        assertEquals(new BigDecimal("1000.00"), saved.getSaldoPendiente());
    }
}
