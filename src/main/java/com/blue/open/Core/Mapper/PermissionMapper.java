package com.blue.open.Core.Mapper;

import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Entity.Db.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper extends AbstractMapperComplete<Permission, PermissionDTO, PermissionDTO> {

    @Override
    public PermissionDTO modelToDto(Permission model) {
        PermissionDTO dto = new PermissionDTO();
        dto.setAbstract(model);
        dto.setName(model.getName());
        return dto;
    }

    @Override
    public Permission dtoToModel(PermissionDTO dto, Permission model) {
        if (model == null) {
            model = new Permission();
        }
        model.setAbstract(dto);
        model.setName(dto.getName());
        return model;
    }

    @Override
    public PermissionDTO modelToPaginateDto(Permission model) {
        return modelToDto(model);
    }
}
