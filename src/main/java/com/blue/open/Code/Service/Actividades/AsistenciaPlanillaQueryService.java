package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaDTO;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Mapper.AsistenciaActividadMapper;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsistenciaPlanillaQueryService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AsistenciaAutorizacionService autorizacionService;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Autowired
    private AsistenciaActividadMapper asistenciaActividadMapper;

    @Transactional(readOnly = true)
    public AsistenciaPlanillaDTO obtenerPlanilla(String slotUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        Slot slot = autorizacionService.requireSlotConsultaProfesor(slotUuid, companyId);
        List<InscripcionActividad> inscripciones = cargarInscripcionesPlanilla(slot, companyId);
        Map<Long, AsistenciaActividad> asistenciaByMemberId = indexarAsistencias(slot.getId());

        return asistenciaActividadMapper.toPlanillaDto(slot, inscripciones, asistenciaByMemberId);
    }

    List<InscripcionActividad> cargarInscripcionesPlanilla(Slot slot, Long companyId) {
        Long actividadId = slot.getActividad().getId();
        LocalDate fechaSlot = slot.getInicio().toLocalDate();
        boolean slotPasado = fechaSlot.isBefore(LocalDate.now());

        List<InscripcionActividad> inscripciones;
        if (slotPasado) {
            inscripciones = inscripcionActividadRepository.findByActividadIdAndCompanyIdAndStatusInWithMember(
                    actividadId, companyId, List.of(0, -1));
        } else {
            inscripciones = inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(
                    actividadId, companyId);
        }

        // Cada clase sale de un horario: la planilla lista solo a quienes eligieron ese horario.
        if (slot.getHorario() != null) {
            Long horarioId = slot.getHorario().getId();
            inscripciones = inscripciones.stream()
                    .filter(inscripcion -> inscripcion.getHorarios().stream()
                            .anyMatch(horario -> horario.getId().equals(horarioId)))
                    .toList();
        }

        return deduplicarPorMember(inscripciones);
    }

    Map<Long, AsistenciaActividad> indexarAsistencias(Long slotId) {
        List<AsistenciaActividad> registros = asistenciaActividadRepository.findBySlotIdWithMember(slotId);
        Map<Long, AsistenciaActividad> asistenciaByMemberId = new HashMap<>();
        for (AsistenciaActividad registro : registros) {
            asistenciaByMemberId.put(registro.getMember().getId(), registro);
        }
        return asistenciaByMemberId;
    }

    private List<InscripcionActividad> deduplicarPorMember(List<InscripcionActividad> inscripciones) {
        Map<Long, InscripcionActividad> porMemberId = new LinkedHashMap<>();
        for (InscripcionActividad inscripcion : inscripciones) {
            Long memberId = inscripcion.getMember().getId();
            InscripcionActividad existente = porMemberId.get(memberId);
            if (existente == null || preferirInscripcion(inscripcion, existente)) {
                porMemberId.put(memberId, inscripcion);
            }
        }
        return new ArrayList<>(porMemberId.values());
    }

    private boolean preferirInscripcion(InscripcionActividad candidata, InscripcionActividad actual) {
        if (actual.getStatus() != 0 && candidata.getStatus() == 0) {
            return true;
        }
        if (actual.getStatus() == 0 && candidata.getStatus() != 0) {
            return false;
        }
        if (candidata.getFechaInscripcion() == null) {
            return false;
        }
        if (actual.getFechaInscripcion() == null) {
            return true;
        }
        return candidata.getFechaInscripcion().isAfter(actual.getFechaInscripcion());
    }
}
