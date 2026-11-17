package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfesorAuthService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorRepository profesorRepository;

    public Profesor getCurrentProfesorOrThrow(Long companyId) {
        User user = authService.getActualOrThrow();
        return profesorRepository.findByUserIdAndCompanyIdWithUser(user.getId(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Profesor no encontrado",
                        "ProfesorAuthService | userId: " + user.getId() + " companyId: " + companyId));
    }

    public Profesor requirePortalEligible(Profesor profesor) {
        if (profesor == null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso al portal no disponible",
                    "ProfesorAuthService | profesor null");
        }
        if (profesor.getEstado() != ProfesorEstadoEnum.ACTIVO) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso al portal no disponible",
                    "ProfesorAuthService | estado: " + profesor.getEstado()
                            + " profesorId: " + profesor.getId());
        }
        return profesor;
    }
}
