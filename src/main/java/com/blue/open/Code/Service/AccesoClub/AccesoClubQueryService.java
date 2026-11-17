package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AccesoClubItemDTO;
import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Mapper.AccesoClubMapper;
import com.blue.open.Code.Repository.AccesoClubRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccesoClubQueryService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccesoClubRepository accesoClubRepository;

    @Autowired
    private AccesoClubMapper accesoClubMapper;

    @Transactional(readOnly = true)
    public List<AccesoClubItemDTO> listarPorRango(LocalDateTime desde, LocalDateTime hasta) {
        authService.valid(List.of(PermissionEnum.CLUB_ACCESS_VIEW));
        validarRango(desde, hasta);

        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        List<AccesoClub> accesos = accesoClubRepository.findByCompanyIdAndFechaBetweenWithUser(
                companyId, desde, hasta);

        List<AccesoClubItemDTO> result = new ArrayList<>(accesos.size());
        for (AccesoClub acceso : accesos) {
            result.add(accesoClubMapper.toItemDto(acceso));
        }
        return result;
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas requerido",
                    "AccesoClubQueryService | desde o hasta null");
        }
        if (desde.isAfter(hasta)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta",
                    "AccesoClubQueryService | desde: " + desde + ", hasta: " + hasta);
        }
    }
}
