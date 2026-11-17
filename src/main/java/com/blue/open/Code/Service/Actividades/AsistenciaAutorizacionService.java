package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AsistenciaAutorizacionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    public Slot requireSlotOperable(String slotUuid, Long companyId, boolean portalProfesor) {
        if (!portalProfesor) {
            rechazarProfesorEnBackoffice(companyId);
            throw new CustomException(HttpStatus.FORBIDDEN, "Operación no permitida en backoffice",
                    "AsistenciaAutorizacionService | mutación solo portal profesor slotUuid: " + slotUuid);
        }

        Slot slot = requireSlotBase(slotUuid, companyId);
        requireProfesorAsignadoActivo(slot.getActividad(), companyId);
        return slot;
    }

    public Slot requireSlotConsultaProfesor(String slotUuid, Long companyId) {
        Slot slot = requireSlotBase(slotUuid, companyId);
        requireProfesorAsignadoActivo(slot.getActividad(), companyId);
        return slot;
    }

    public Actividad requireActividadConsulta(String actividadUuid, Long companyId, boolean portalProfesor) {
        Actividad actividad = actividadRepository.findByUuidAndCompany_IdAndStatus(actividadUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "Actividad no disponible",
                        "AsistenciaAutorizacionService | actividad inactiva o inexistente uuid: " + actividadUuid));

        if (portalProfesor) {
            requireProfesorAsignadoActivo(actividad, companyId);
        }
        return actividad;
    }

    private Slot requireSlotBase(String slotUuid, Long companyId) {
        Slot slot = slotRepository.findByUuidAndCompanyIdWithActividad(slotUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Slot no encontrado",
                        "AsistenciaAutorizacionService | uuid: " + slotUuid));

        if (slot.getEstado() == SlotEstadoEnum.CANCELADO) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Slot cancelado",
                    "AsistenciaAutorizacionService | slot cancelado uuid: " + slotUuid);
        }

        Actividad actividad = slot.getActividad();
        if (actividad == null || actividad.getStatus() != 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Actividad no disponible",
                    "AsistenciaAutorizacionService | actividad inactiva slotUuid: " + slotUuid);
        }

        return slot;
    }

    private void requireProfesorAsignadoActivo(Actividad actividad, Long companyId) {
        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(companyId));

        if (!actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(
                actividad.getId(), profesor.getId(), STATUS_ACTIVE)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Actividad fuera de alcance",
                    "AsistenciaAutorizacionService | profesor no asignado actividadId: "
                            + actividad.getId() + " profesorId: " + profesor.getId());
        }
    }

    private void rechazarProfesorEnBackoffice(Long companyId) {
        User user = authService.getActualOrThrow();
        if (profesorRepository.findByUserIdAndCompanyIdWithUser(user.getId(), companyId).isPresent()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Operación no permitida en backoffice",
                    "AsistenciaAutorizacionService | profesor en backoffice userId: " + user.getId());
        }
    }
}
