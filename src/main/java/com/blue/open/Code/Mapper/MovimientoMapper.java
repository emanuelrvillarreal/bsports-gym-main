package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoRowDTO;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MovimientoOrigenEnum;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class MovimientoMapper {

    public MovimientoRowDTO toRow(Movimiento movimiento) {
        MovimientoRowDTO dto = new MovimientoRowDTO();
        mapCommon(movimiento, dto);
        return dto;
    }

    public MovimientoDetalleDTO toDetalle(Movimiento movimiento) {
        MovimientoDetalleDTO dto = new MovimientoDetalleDTO();
        mapCommon(movimiento, dto);
        User anuladaPor = movimiento.getAnuladaPor();
        if (anuladaPor != null) {
            dto.setAnuladaPorUuid(anuladaPor.getUuid());
        }
        dto.setAnuladaMotivo(movimiento.getAnuladaMotivo());
        Timestamp anuladaAt = movimiento.getAnuladaAt();
        if (anuladaAt != null) {
            dto.setAnuladaAt(anuladaAt.toLocalDateTime());
        }
        return dto;
    }

    private void mapCommon(Movimiento movimiento, MovimientoRowDTO dto) {
        dto.setMovimientoUuid(movimiento.getUuid());
        dto.setTipo(movimiento.getTipo());
        dto.setMonto(movimiento.getMonto());
        dto.setFecha(movimiento.getFecha());
        dto.setConcepto(movimiento.getConcepto());
        dto.setMedioPago(movimiento.getMedioPago());
        dto.setObservaciones(movimiento.getObservaciones());
        dto.setEstado(movimiento.getEstado());

        Cuota cuota = movimiento.getCuota();
        if (cuota != null) {
            dto.setOrigen(MovimientoOrigenEnum.COBRO_CUOTA);
            dto.setCuotaUuid(cuota.getUuid());
        } else if (movimiento.getReserva() != null) {
            dto.setOrigen(MovimientoOrigenEnum.COBRO_RESERVA);
            dto.setReservaUuid(movimiento.getReserva().getUuid());
        } else {
            dto.setOrigen(MovimientoOrigenEnum.MANUAL);
        }

        Member persona = movimiento.getPersona();
        if (persona != null) {
            dto.setPersonaMemberUuid(persona.getUuid());
            User user = persona.getUser();
            if (user != null) {
                dto.setPersonaNombre(user.getNombre());
                dto.setPersonaApellido(user.getApellido());
            }
        }

        Member titular = movimiento.getTitular();
        if (titular != null) {
            dto.setTitularMemberUuid(titular.getUuid());
        }

        User registradoPor = movimiento.getRegistradoPor();
        if (registradoPor != null) {
            dto.setRegistradoPorUuid(registradoPor.getUuid());
            dto.setRegistradoPorNombre(registradoPor.getNombre());
            dto.setRegistradoPorApellido(registradoPor.getApellido());
        }
    }
}
