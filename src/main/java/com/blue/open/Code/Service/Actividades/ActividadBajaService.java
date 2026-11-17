package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActividadBajaService {

    private static final int STATUS_DELETED = -1;
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
    private SlotRepository slotRepository;

    @Transactional
    public void bajaLogica(String actividadUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_DELETE));
        Company company = authService.getCurrentCompanyOrThrow();

        Actividad actividad = actividadRepository.findByUuidAndCompany_Id(actividadUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "ActividadBajaService | uuid: " + actividadUuid));

        if (actividad.getStatus() != null && actividad.getStatus() == STATUS_DELETED) {
            return;
        }

        if (inscripcionActividadRepository.countByActividad_IdAndStatus(actividad.getId(), STATUS_ACTIVE) > 0) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "No se puede eliminar la actividad: tiene socios inscriptos activos",
                    "ActividadBajaService | uuid: " + actividadUuid);
        }

        actividadProfesorRepository.bajaLogicaByActividadId(actividad.getId());
        slotRepository.deleteByActividad_IdAndInicioGreaterThanEqual(actividad.getId(), LocalDateTime.now());
        actividad.setStatus(STATUS_DELETED);
        actividadRepository.save(actividad);
    }
}
