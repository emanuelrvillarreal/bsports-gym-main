package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.AccesoClubItemDTO;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Entity.DTO.ClubPresenciaItemDTO;
import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Entity.Db.User;
import org.springframework.stereotype.Component;

@Component
public class AccesoClubMapper {

    public AccesoClubRegistroDTO toRegistroDto(AccesoClub acceso, boolean presente, String advertencia) {
        AccesoClubRegistroDTO dto = new AccesoClubRegistroDTO();
        dto.setAccesoUuid(acceso.getUuid());
        dto.setTipo(acceso.getTipo());
        dto.setMemberEstado(acceso.getMemberEstado());
        dto.setAdvertencia(advertencia);
        dto.setFecha(acceso.getFecha());
        dto.setPresente(presente);
        mapUser(acceso.getUser(), dto);
        return dto;
    }

    public AccesoClubItemDTO toItemDto(AccesoClub acceso) {
        AccesoClubItemDTO dto = new AccesoClubItemDTO();
        dto.setAccesoUuid(acceso.getUuid());
        dto.setTipo(acceso.getTipo());
        dto.setMemberEstado(acceso.getMemberEstado());
        dto.setFecha(acceso.getFecha());
        mapUser(acceso.getUser(), dto);
        return dto;
    }

    public ClubPresenciaItemDTO toPresenciaItemDto(ClubPresencia presencia) {
        ClubPresenciaItemDTO dto = new ClubPresenciaItemDTO();
        dto.setPresente(presencia.isPresente());
        dto.setUltimoAccesoAt(presencia.getUltimoAccesoAt());
        mapUser(presencia.getUser(), dto);
        return dto;
    }

    private void mapUser(User user, AccesoClubRegistroDTO dto) {
        if (user == null) {
            return;
        }
        dto.setUserUuid(user.getUuid());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setDni(user.getDni());
    }

    private void mapUser(User user, AccesoClubItemDTO dto) {
        if (user == null) {
            return;
        }
        dto.setUserUuid(user.getUuid());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setDni(user.getDni());
    }

    private void mapUser(User user, ClubPresenciaItemDTO dto) {
        if (user == null) {
            return;
        }
        dto.setUserUuid(user.getUuid());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setDni(user.getDni());
        dto.setImageUrl(user.getImageUrl());
    }
}
