package com.blue.open.Core.Mapper;

import com.blue.open.Core.Entity.DTO.UserAccessTokenDTO;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import org.springframework.stereotype.Component;

@Component
public class UserAccessTokenMapper extends AbstractMapperComplete<UserAccessToken, UserAccessTokenDTO, UserAccessTokenDTO> {

    @Override
    public UserAccessTokenDTO modelToDto(UserAccessToken model) {
        if (model == null) return null;
        UserAccessTokenDTO dto = new UserAccessTokenDTO();
        dto.setAbstract(model);
        dto.setUuid(model.getUuid());
        dto.setName(model.getName());
        dto.setCode(model.getCode());
        dto.setActive(model.getActive());
        if (model.getUser() != null) {
            dto.setUserUuid(model.getUser().getUuid());
        }
        return dto;
    }

    @Override
    public UserAccessToken dtoToModel(UserAccessTokenDTO dto, UserAccessToken model) {
        if (dto == null) return model;
        if (model == null) {
            model = new UserAccessToken();
        }
        model.setAbstract(dto);
        model.setName(dto.getName());
        model.setCode(dto.getCode());
        model.setActive(dto.getActive());
        return model;
    }

    @Override
    public UserAccessTokenDTO modelToPaginateDto(UserAccessToken model) {
        return modelToDto(model);
    }
}
