package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoActividadAutorizacionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private MemberAuthService memberAuthService;

    public Actividad requireActividadActivaParaAlta(String actividadUuid, Long companyId) {
        return actividadRepository.findByUuidAndCompany_IdAndStatus(actividadUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "Actividad no disponible",
                        "EventoActividadAutorizacionService | actividad inactiva o inexistente uuid: "
                                + actividadUuid));
    }

    public void requireAlcanceStaffOProfesorAsignado(Actividad actividad, Long companyId, boolean portalProfesor) {
        if (!portalProfesor) {
            rechazarProfesorEnBackoffice(companyId);
            return;
        }

        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(companyId));

        if (!actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(
                actividad.getId(), profesor.getId(), STATUS_ACTIVE)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Actividad fuera de alcance",
                    "EventoActividadAutorizacionService | profesor no asignado actividadId: "
                            + actividad.getId() + " profesorId: " + profesor.getId());
        }
    }

    public void requireVisibilidadDetalle(EventoActividad evento, Long companyId) {
        if (authService.validBoolean(List.of(PermissionEnum.ACTIVITY_VIEW))) {
            return;
        }

        User user = authService.getActualOrThrow();
        Actividad actividad = evento.getActividad();

        if (profesorRepository.findByUserIdAndCompanyIdWithUser(user.getId(), companyId).isPresent()) {
            Profesor profesor = profesorAuthService.getCurrentProfesorOrThrow(companyId);
            if (actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(
                    actividad.getId(), profesor.getId(), STATUS_ACTIVE)) {
                return;
            }
        }

        Member member = memberAuthService.getCurrentMemberOrThrow(companyId);
        if (inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(
                actividad.getId(), member.getId(), 0)) {
            return;
        }

        throw new CustomException(HttpStatus.FORBIDDEN, "Evento no visible",
                "EventoActividadAutorizacionService | sin visibilidad eventoUuid: " + evento.getUuid());
    }

    private void rechazarProfesorEnBackoffice(Long companyId) {
        User user = authService.getActualOrThrow();
        if (profesorRepository.findByUserIdAndCompanyIdWithUser(user.getId(), companyId).isPresent()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Operación no permitida en backoffice",
                    "EventoActividadAutorizacionService | profesor en backoffice userId: " + user.getId());
        }
    }
}
