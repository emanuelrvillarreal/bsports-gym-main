package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import org.springframework.stereotype.Component;

@Component
public class AmenityMapper {

    public AmenityDTO toDto(Amenity amenity) {
        if (amenity == null) {
            return null;
        }
        AmenityDTO dto = new AmenityDTO();
        dto.setAmenityUuid(amenity.getUuid());
        dto.setNombre(amenity.getNombre());
        dto.setMontoHora(amenity.getMontoHora());
        dto.setPorcentajeSena(amenity.getPorcentajeSena());
        dto.setDiasSemana(amenity.getDiasSemana());
        dto.setHoraInicio(amenity.getHoraInicio());
        dto.setHoraFin(amenity.getHoraFin());
        return dto;
    }

    public ReservaAmenityDTO reservaToDto(Reserva reserva) {
        if (reserva == null) {
            return null;
        }
        ReservaAmenityDTO dto = new ReservaAmenityDTO();
        dto.setReservaUuid(reserva.getUuid());
        dto.setMontoTotal(reserva.getMontoTotal());
        dto.setMontoSena(reserva.getMontoSena());
        dto.setSaldoPendiente(reserva.getSaldoPendiente());
        dto.setEstado(reserva.getEstado());
        if (reserva.getVenceSenaAt() != null) {
            dto.setVenceSenaAt(reserva.getVenceSenaAt().toLocalDateTime());
        }

        Amenity amenity = reserva.getAmenity();
        if (amenity != null) {
            dto.setAmenityUuid(amenity.getUuid());
            dto.setAmenityNombre(amenity.getNombre());
        }

        Member member = reserva.getMember();
        if (member != null) {
            dto.setMemberUuid(member.getUuid());
            User user = member.getUser();
            if (user != null) {
                dto.setMemberNombre(user.getNombre());
                dto.setMemberApellido(user.getApellido());
            }
        }

        Slot slot = reserva.getSlot();
        if (slot != null) {
            dto.setSlotUuid(slot.getUuid());
            dto.setSlotInicio(slot.getInicio());
            dto.setSlotFin(slot.getFin());
            dto.setSlotEstado(slot.getEstado());
        }

        User canceladaPor = reserva.getCanceladaPor();
        if (canceladaPor != null) {
            dto.setCanceladaPorUuid(canceladaPor.getUuid());
        }
        if (reserva.getCanceladaAt() != null) {
            dto.setCanceladaAt(reserva.getCanceladaAt().toLocalDateTime());
        }
        return dto;
    }
}
