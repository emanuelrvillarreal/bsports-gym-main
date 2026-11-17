package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.AsistenciaActividadConsultaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaRowDTO;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AsistenciaActividadMapper {

    public AsistenciaPlanillaDTO toPlanillaDto(
            Slot slot,
            List<InscripcionActividad> inscripciones,
            Map<Long, AsistenciaActividad> asistenciaByMemberId) {
        AsistenciaPlanillaDTO dto = new AsistenciaPlanillaDTO();
        dto.setSlotUuid(slot.getUuid());
        dto.setFecha(slot.getInicio().toLocalDate());
        dto.setInicio(slot.getInicio());
        dto.setFin(slot.getFin());
        if (slot.getActividad() != null) {
            dto.setActividadUuid(slot.getActividad().getUuid());
            dto.setActividadNombre(slot.getActividad().getNombre());
        }

        List<AsistenciaPlanillaRowDTO> rows = new ArrayList<>();
        for (InscripcionActividad inscripcion : inscripciones) {
            rows.add(toPlanillaRowDto(inscripcion, asistenciaByMemberId.get(inscripcion.getMember().getId())));
        }
        dto.setAlumnos(rows);
        return dto;
    }

    public AsistenciaPlanillaRowDTO toPlanillaRowDto(InscripcionActividad inscripcion, AsistenciaActividad asistencia) {
        AsistenciaPlanillaRowDTO row = new AsistenciaPlanillaRowDTO();
        Member member = inscripcion.getMember();
        row.setMemberUuid(member.getUuid());
        row.setFechaInscripcion(inscripcion.getFechaInscripcion());
        User user = member.getUser();
        if (user != null) {
            row.setMemberNombre(user.getNombre());
            row.setMemberApellido(user.getApellido());
        }
        if (asistencia != null) {
            row.setEstado(asistencia.getEstado());
        }
        return row;
    }

    public AsistenciaActividadConsultaDTO toConsultaDto(AsistenciaActividad asistencia) {
        AsistenciaActividadConsultaDTO dto = new AsistenciaActividadConsultaDTO();
        dto.setUuid(asistencia.getUuid());
        dto.setFecha(asistencia.getFecha());
        dto.setEstado(asistencia.getEstado());

        Slot slot = asistencia.getSlot();
        if (slot != null) {
            dto.setSlotUuid(slot.getUuid());
            dto.setSlotInicio(slot.getInicio());
        }

        if (asistencia.getActividad() != null) {
            dto.setActividadUuid(asistencia.getActividad().getUuid());
            dto.setActividadNombre(asistencia.getActividad().getNombre());
        }

        Member member = asistencia.getMember();
        if (member != null) {
            dto.setMemberUuid(member.getUuid());
            User user = member.getUser();
            if (user != null) {
                dto.setMemberNombre(user.getNombre());
                dto.setMemberApellido(user.getApellido());
            }
        }
        return dto;
    }

    public List<AsistenciaActividadConsultaDTO> toConsultaDtos(List<AsistenciaActividad> asistencias) {
        List<AsistenciaActividadConsultaDTO> dtos = new ArrayList<>();
        for (AsistenciaActividad asistencia : asistencias) {
            dtos.add(toConsultaDto(asistencia));
        }
        return dtos;
    }
}
