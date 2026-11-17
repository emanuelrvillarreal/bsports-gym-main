package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Entity.DTO.CalendarioSlotTipoEnum;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarioQueryService {

    @Autowired
    private AuthService authService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional(readOnly = true)
    public List<CalendarioSlotDTO> listar(LocalDateTime desde, LocalDateTime hasta) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_VIEW, PermissionEnum.AMENITY_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        validarRango(desde, hasta);

        List<Slot> slots = slotRepository.findByCompanyAndInicioBetweenWithDetails(companyId, desde, hasta);
        Map<Long, Reserva> reservaPorSlotId = buscarReservasPorSlot(companyId, slots);
        List<EventoActividad> eventos = eventoActividadRepository.findByCompanyAndInicioBetweenWithDetails(
                companyId, desde, hasta);

        List<CalendarioSlotDTO> resultado = new ArrayList<>();
        slots.forEach(slot -> resultado.add(toDto(slot, reservaPorSlotId.get(slot.getId()))));
        eventos.forEach(evento -> resultado.add(eventoActividadMapper.toCalendarioDto(evento)));

        resultado.sort(Comparator.comparing(CalendarioSlotDTO::getInicio));
        return resultado;
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido",
                    "CalendarioQueryService | desde/hasta inválidos");
        }
    }

    private Map<Long, Reserva> buscarReservasPorSlot(Long companyId, List<Slot> slots) {
        List<Long> idsSlotsDeAmenities = slots.stream()
                .filter(slot -> slot.getAmenity() != null)
                .map(Slot::getId)
                .toList();

        if (idsSlotsDeAmenities.isEmpty()) {
            return Map.of();
        }

        List<Reserva> reservas = reservaRepository.findByCompanyIdAndSlotIdIn(companyId, idsSlotsDeAmenities);
        return reservas.stream()
                .collect(Collectors.toMap(reserva -> reserva.getSlot().getId(), reserva -> reserva));
    }

    private CalendarioSlotDTO toDto(Slot slot, Reserva reserva) {
        CalendarioSlotDTO dto = new CalendarioSlotDTO();
        dto.setSlotUuid(slot.getUuid());
        dto.setInicio(slot.getInicio());
        dto.setFin(slot.getFin());
        dto.setEstado(slot.getEstado());

        if (slot.getAmenity() != null) {
            completarDtoAmenity(dto, slot.getAmenity(), reserva);
        } else if (slot.getActividad() != null) {
            completarDtoActividad(dto, slot.getActividad());
        }

        return dto;
    }

    private void completarDtoAmenity(CalendarioSlotDTO dto, Amenity amenity, Reserva reserva) {
        dto.setTipo(CalendarioSlotTipoEnum.AMENITY);
        dto.setReferenciaUuid(amenity.getUuid());
        dto.setReferenciaNombre(amenity.getNombre());
        completarTitular(dto, reserva);
    }

    private void completarDtoActividad(CalendarioSlotDTO dto, Actividad actividad) {
        dto.setTipo(CalendarioSlotTipoEnum.ACTIVIDAD);
        dto.setReferenciaUuid(actividad.getUuid());
        dto.setReferenciaNombre(actividad.getNombre());
    }

    private void completarTitular(CalendarioSlotDTO dto, Reserva reserva) {
        if (reserva == null || reserva.getMember() == null || reserva.getMember().getUser() == null) {
            return;
        }
        User user = reserva.getMember().getUser();
        dto.setTitularNombre(user.getNombre());
        dto.setTitularApellido(user.getApellido());
    }
}
