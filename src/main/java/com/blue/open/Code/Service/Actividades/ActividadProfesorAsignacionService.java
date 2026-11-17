package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActividadProfesorAsignacionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Transactional
    public void asignar(Actividad actividad, Profesor profesor) {
        validatePair(actividad, profesor);
        if (actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(
                actividad.getId(), profesor.getId(), STATUS_ACTIVE)) {
            return;
        }

        actividadProfesorRepository.findByActividadIdAndProfesorId(actividad.getId(), profesor.getId())
                .ifPresentOrElse(
                        link -> {
                            link.setStatus(STATUS_ACTIVE);
                            actividadProfesorRepository.save(link);
                        },
                        () -> {
                            ActividadProfesor link = new ActividadProfesor();
                            link.setActividadId(actividad.getId());
                            link.setProfesorId(profesor.getId());
                            link.setStatus(STATUS_ACTIVE);
                            actividadProfesorRepository.save(link);
                        });
    }

    @Transactional
    public void desasignar(Actividad actividad, Profesor profesor) {
        validatePair(actividad, profesor);
        actividadProfesorRepository.bajaLogicaByActividadIdAndProfesorId(actividad.getId(), profesor.getId());
    }

    private void validatePair(Actividad actividad, Profesor profesor) {
        if (actividad == null || actividad.getId() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Actividad inválida",
                    "ActividadProfesorAsignacionService | actividad null");
        }
        if (profesor == null || profesor.getId() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Profesor inválido",
                    "ActividadProfesorAsignacionService | profesor null");
        }
        if (!actividad.getCompany().getId().equals(profesor.getCompany().getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El profesor pertenece a otro gimnasio",
                    "ActividadProfesorAsignacionService | company mismatch");
        }
    }
}
