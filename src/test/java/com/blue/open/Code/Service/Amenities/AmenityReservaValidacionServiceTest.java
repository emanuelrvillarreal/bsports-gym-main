package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmenityReservaValidacionServiceTest {

    @Mock
    private MemberAuthService memberAuthService;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SlotRepository slotRepository;

    private AmenityReservaValidacionService service;

    @BeforeEach
    void setUp() {
        service = new AmenityReservaValidacionService();
        ReflectionTestUtils.setField(service, "memberAuthService", memberAuthService);
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "slotRepository", slotRepository);
    }

    // RF-04 / CA-04 / TC-12: Dado familiar no titular, cuando validarTitular, entonces rechaza
    @Test
    void validarTitular_rechazaFamiliarNoTitular() {
        Member familiar = new Member();
        familiar.setId(2L);
        Member titular = new Member();
        titular.setId(1L);
        familiar.setTitular(titular);

        when(memberAuthService.resolveMemberByUuid("fam-uuid", 1L)).thenReturn(familiar);

        assertThrows(CustomException.class, () -> service.validarTitular("fam-uuid", 1L));
    }

    // RF-04 / CA-04 / TC-13: Dado 2 reservas en la semana, cuando validarTopeSemanal, entonces rechaza tercera
    @Test
    void validarTopeSemanal_rechazaTerceraReserva() {
        LocalDateTime inicio = nextSaturdayAt(LocalTime.of(14, 0));
        when(reservaRepository.countByMemberAndSemanaCalendario(
                eq(1L), eq(10L), eq(List.of(ReservaEstadoEnum.PENDIENTE_SENA, ReservaEstadoEnum.CONFIRMADA)),
                any(), any()))
                .thenReturn(2L);

        assertThrows(CustomException.class,
                () -> service.validarTopeSemanal(1L, 10L, inicio));
    }

    // RF-05 / CA-05 / TC-14: Dado slot solapado mismo amenity, cuando validarSolapamiento, entonces rechaza
    @Test
    void validarSolapamiento_rechazaCuandoExisteConflicto() {
        when(slotRepository.existsSolapamientoAmenity(
                eq(5L), eq(List.of(SlotEstadoEnum.PENDIENTE_DE_PAGO, SlotEstadoEnum.CONFIRMADO)),
                any(), any()))
                .thenReturn(true);

        assertThrows(CustomException.class,
                () -> service.validarSolapamiento(5L, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(2)));
    }

    // RF-05 / CA-05 / TC-15: Dado día no disponible en amenity, cuando validarHorarioAmenity, entonces rechaza
    @Test
    void validarHorarioAmenity_rechazaDiaNoDisponible() {
        Amenity amenity = new Amenity();
        amenity.setDiasSemana("SABADO");
        amenity.setHoraInicio(LocalTime.of(10, 0));
        amenity.setHoraFin(LocalTime.of(18, 0));

        LocalDateTime domingo = nextDayOfWeek(java.time.DayOfWeek.SUNDAY, LocalTime.of(14, 0));

        assertThrows(CustomException.class,
                () -> service.validarHorarioAmenity(amenity, domingo, domingo.plusHours(2)));
    }

    private LocalDateTime nextSaturdayAt(LocalTime time) {
        return nextDayOfWeek(java.time.DayOfWeek.SATURDAY, time);
    }

    private LocalDateTime nextDayOfWeek(java.time.DayOfWeek day, LocalTime time) {
        LocalDate date = LocalDate.now().plusDays(2);
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date.atTime(time);
    }
}
