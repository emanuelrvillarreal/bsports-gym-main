package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Event.CuotaSocialRangoBajaEvent;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuotaSocialRangoBajaService {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void bajaLogica(String cuotaSocialRangoUuid) {
        Company company = authService.getCurrentCompanyOrThrow();

        CuotaSocialRango rango = cuotaSocialRangoRepository
                .findByUuidAndCompanyIdAndStatusActive(cuotaSocialRangoUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Rango de cuota social no encontrado",
                        "CuotaSocialRangoBajaService | uuid: " + cuotaSocialRangoUuid));

        if (Boolean.TRUE.equals(rango.getEsDefault())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se puede eliminar el rango por defecto del gimnasio",
                    "CuotaSocialRangoBajaService | uuid: " + cuotaSocialRangoUuid);
        }

        rango.setStatus(STATUS_DELETED);
        cuotaSocialRangoRepository.save(rango);

        eventPublisher.publishEvent(new CuotaSocialRangoBajaEvent(
                company.getId(), rango.getId(), rango.getUuid()));
    }
}
