package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfesorBajaService {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Transactional
    public void bajaLogica(String profesorUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_DELETE));
        Company company = authService.getCurrentCompanyOrThrow();

        Profesor profesor = profesorRepository.findByUuidAndCompanyId(profesorUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Profesor no encontrado",
                        "ProfesorBajaService | uuid: " + profesorUuid));

        if (profesor.getStatus() != null && profesor.getStatus() == STATUS_DELETED) {
            return;
        }

        actividadProfesorRepository.bajaLogicaByProfesorId(profesor.getId());
        profesor.setEstado(ProfesorEstadoEnum.INACTIVO);
        profesor.setStatus(STATUS_DELETED);
        profesorRepository.save(profesor);
    }
}
