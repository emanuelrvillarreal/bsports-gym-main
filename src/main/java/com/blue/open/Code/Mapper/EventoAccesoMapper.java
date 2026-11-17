package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.EventoAccesoDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanResultDTO;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.EventoAccesoHistorial;
import com.blue.open.Code.Entity.Db.Member;
import org.springframework.stereotype.Component;

@Component
public class EventoAccesoMapper {

    public EventoAccesoDetalleDTO toDetalleDto(EventoAcceso evento) {
        EventoAccesoDetalleDTO dto = new EventoAccesoDetalleDTO();
        dto.setUuid(evento.getUuid());
        dto.setTitulo(evento.getTitulo());
        dto.setDescripcion(evento.getDescripcion());
        dto.setMonto(evento.getMonto());
        dto.setFecha(evento.getFecha());
        dto.setPermiteMorosos(evento.getPermiteMorosos());
        dto.setPermiteInvitados(evento.getPermiteInvitados());
        dto.setMontoMorosos(evento.getMontoMorosos());
        dto.setMontoInvitados(evento.getMontoInvitados());
        return dto;
    }

    public EventoAccesoScanResultDTO toScanResultDto(Member member) {
        EventoAccesoScanResultDTO dto = new EventoAccesoScanResultDTO();
        dto.setMemberUuid(member.getUuid());
        if (member.getUser() != null) {
            dto.setNombre(member.getUser().getNombre());
            dto.setApellido(member.getUser().getApellido());
        }
        dto.setNumeroSocio(member.getNumeroSocio());
        return dto;
    }

    public EventoAccesoHistorialDetalleDTO toHistorialDetalleDto(EventoAccesoHistorial historial) {
        EventoAccesoHistorialDetalleDTO dto = new EventoAccesoHistorialDetalleDTO();
        dto.setUuid(historial.getUuid());
        dto.setTipo(historial.getTipo());
        dto.setNombre(historial.getNombre());
        dto.setResultado(historial.getResultado());
        dto.setMotivo(historial.getMotivo());
        dto.setMonto(historial.getMonto());
        dto.setFecha(historial.getDateCreated());
        if (historial.getMember() != null) {
            dto.setMemberUuid(historial.getMember().getUuid());
            dto.setNumeroSocio(historial.getMember().getNumeroSocio());
        }
        return dto;
    }
}
