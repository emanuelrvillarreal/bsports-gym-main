package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.PortalReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.DTO.ReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaQueryServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final int MAX_PAGE_SIZE = 100;

    @Mock
    private ReservaRepository reservaRepository;

    private ReservaQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReservaQueryService();
        ReflectionTestUtils.setField(service, "reservaRepository", reservaRepository);
        ReflectionTestUtils.setField(service, "amenityMapper", new AmenityMapper());
    }

    @SuppressWarnings("unchecked")
    @Test
    void paginar_devuelveReservasMapeadas() {
        Amenity amenity = new Amenity();
        amenity.setUuid("amenity-uuid");
        amenity.setNombre("Pileta");

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setMontoTotal(new BigDecimal("500.00"));
        reserva.setMontoSena(new BigDecimal("100.00"));
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reserva.setAmenity(amenity);

        when(reservaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reserva)));

        Page<ReservaAmenityDTO> result = service.paginar(new ReservaPaginateRequestDTO(), COMPANY_ID);

        assertEquals(1, result.getTotalElements());
        assertEquals("reserva-uuid", result.getContent().get(0).getReservaUuid());
        assertEquals("Pileta", result.getContent().get(0).getAmenityNombre());
    }

    @Test
    void paginarPorMember_consultaSoloLasReservasDelMemberYLasMapea() {
        Member member = member(MEMBER_ID, "member-uuid");

        when(reservaRepository.paginateByCompanyIdAndMemberId(anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        reservaDe(member, "reserva-1"),
                        reservaDe(member, "reserva-2"))));

        Page<ReservaAmenityDTO> result =
                service.paginarPorMember(new PortalReservaPaginateRequestDTO(), COMPANY_ID, MEMBER_ID);

        ArgumentCaptor<Long> companyIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(reservaRepository).paginateByCompanyIdAndMemberId(
                companyIdCaptor.capture(), memberIdCaptor.capture(), any(Pageable.class));

        // La consulta queda acotada al member recibido: ninguna reserva de otro socio puede entrar.
        assertEquals(COMPANY_ID, companyIdCaptor.getValue());
        assertEquals(MEMBER_ID, memberIdCaptor.getValue());

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(dto -> "member-uuid".equals(dto.getMemberUuid())));
    }

    @Test
    void paginarPorMember_aplicaPaginadoSolicitado() {
        PortalReservaPaginateRequestDTO request = new PortalReservaPaginateRequestDTO();
        request.setPage(3);
        request.setSize(500);

        when(reservaRepository.paginateByCompanyIdAndMemberId(anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.paginarPorMember(request, COMPANY_ID, MEMBER_ID);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaRepository).paginateByCompanyIdAndMemberId(
                anyLong(), anyLong(), pageableCaptor.capture());

        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(MAX_PAGE_SIZE, pageableCaptor.getValue().getPageSize());
    }

    private Member member(Long id, String uuid) {
        User user = new User();
        user.setNombre("Juan");
        user.setApellido("Pérez");

        Member member = new Member();
        member.setId(id);
        member.setUuid(uuid);
        member.setUser(user);
        return member;
    }

    private Reserva reservaDe(Member member, String uuid) {
        Amenity amenity = new Amenity();
        amenity.setUuid("amenity-uuid");
        amenity.setNombre("Cancha 1");

        Slot slot = new Slot();
        slot.setUuid("slot-" + uuid);
        slot.setInicio(LocalDateTime.of(2026, 8, 20, 18, 0));
        slot.setFin(LocalDateTime.of(2026, 8, 20, 19, 0));
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);

        Reserva reserva = new Reserva();
        reserva.setUuid(uuid);
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setMontoTotal(new BigDecimal("1000.00"));
        reserva.setMontoSena(new BigDecimal("300.00"));
        reserva.setSaldoPendiente(new BigDecimal("700.00"));
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);
        reserva.setMember(member);
        return reserva;
    }
}
