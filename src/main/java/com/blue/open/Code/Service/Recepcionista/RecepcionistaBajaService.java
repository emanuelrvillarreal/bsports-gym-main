package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecepcionistaBajaService {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Transactional
    public void bajaLogica(String recepcionistaUuid) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();

        Recepcionista recepcionista = recepcionistaRepository.findByUuidAndCompanyId(recepcionistaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado",
                        "RecepcionistaBajaService | uuid: " + recepcionistaUuid));

        if (recepcionista.getStatus() != null && recepcionista.getStatus() == STATUS_DELETED) {
            return;
        }

        recepcionista.setEstado(RecepcionistaEstadoEnum.INACTIVO);
        recepcionista.setStatus(STATUS_DELETED);
        recepcionistaRepository.save(recepcionista);
    }
}
