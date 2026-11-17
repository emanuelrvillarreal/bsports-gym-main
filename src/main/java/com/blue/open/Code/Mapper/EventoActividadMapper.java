package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;

import org.springframework.stereotype.Component;

@Component
public class EventoActividadMapper {

    public EventoActividadDetalleDTO toDetalleDto(EventoActividad evento) {
        EventoActividadDetalleDTO dto = new EventoActividadDetalleDTO();
        dto.setUuid(evento.getUuid());
        dto.setTitulo(evento.getTitulo());
        dto.setDescripcion(evento.getDescripcion());
        dto.setUbicacion(evento.getUbicacion());
        dto.setInicio(evento.getInicio());
        dto.setFin(evento.getFin());
        dto.setEstado(evento.getEstado());
        if (evento.getActividad() != null) {
            dto.setActividadUuid(evento.getActividad().getUuid());
            dto.setActividadNombre(evento.getActividad().getNombre());
        }
        return dto;
    }

    public CalendarioSlotDTO toCalendarioDto(EventoActividad evento) {
        CalendarioSlotDTO dto = new CalendarioSlotDTO();
        dto.setEventoUuid(evento.getUuid());
        dto.setTitulo(evento.getTitulo());
        dto.setUbicacion(evento.getUbicacion());
        dto.setInicio(evento.getInicio());
        dto.setFin(evento.getFin());
        dto.setEstado(mapEstadoCalendario(evento.getEstado()));
        if (evento.getActividad() != null) {
            dto.setReferenciaUuid(evento.getActividad().getUuid());
            dto.setReferenciaNombre(evento.getActividad().getNombre());
        }
        return dto;
    }

    private SlotEstadoEnum mapEstadoCalendario(EventoActividadEstadoEnum estado) {
        if (estado == EventoActividadEstadoEnum.CANCELADO) {
            return SlotEstadoEnum.CANCELADO;
        }
        return SlotEstadoEnum.CONFIRMADO;
    }
}
