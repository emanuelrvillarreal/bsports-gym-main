package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ClubPresenciaItemDTO;
import com.blue.open.Code.Entity.DTO.ClubPresenciaListDTO;
import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Mapper.AccesoClubMapper;
import com.blue.open.Code.Repository.ClubPresenciaRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClubPresenciaQueryService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ClubPresenciaRepository clubPresenciaRepository;

    @Autowired
    private AccesoClubMapper accesoClubMapper;

    @Transactional(readOnly = true)
    public ClubPresenciaListDTO listarPorPresente(boolean presente) {
        authService.valid(List.of(PermissionEnum.CLUB_ACCESS_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        List<ClubPresencia> presencias = clubPresenciaRepository.findByCompanyIdAndPresenteWithUser(
                companyId, presente);

        List<ClubPresenciaItemDTO> result = new ArrayList<>(presencias.size());
        for (ClubPresencia presencia : presencias) {
            result.add(accesoClubMapper.toPresenciaItemDto(presencia));
        }
        return new ClubPresenciaListDTO(result);
    }
}
