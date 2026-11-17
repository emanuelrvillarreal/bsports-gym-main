package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RecepcionistaAuthService {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    public Recepcionista getCurrentRecepcionistaOrThrow(Long companyId) {
        User user = authService.getActualOrThrow();
        return recepcionistaRepository.findByUserIdAndCompanyIdWithUser(user.getId(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado",
                        "RecepcionistaAuthService | userId: " + user.getId() + " companyId: " + companyId));
    }

    public Recepcionista requirePortalEligible(Recepcionista recepcionista) {
        if (recepcionista == null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso no disponible",
                    "RecepcionistaAuthService | recepcionista null");
        }
        if (recepcionista.getEstado() != RecepcionistaEstadoEnum.ACTIVO) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso no disponible",
                    "RecepcionistaAuthService | estado: " + recepcionista.getEstado()
                            + " recepcionistaId: " + recepcionista.getId());
        }
        return recepcionista;
    }
}
