package com.blue.open.Core.Mapper;

import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper extends AbstractMapperComplete<Role, RoleDTO, RoleDTO> {

    @Autowired
    protected PermissionMapper permissionMapper;

    @Override
    public RoleDTO modelToDto(Role model) {
        RoleDTO dto = new RoleDTO();
        dto.setAbstract(model);
        dto.setName(model.getName());
        dto.setIsDefault(model.getIsDefault());
        if (model.getPermissions() != null) {
            dto.setPermissions(permissionMapper.modelsToDtos(model.getPermissions()));
        }
        return dto;
    }

    @Override
    public Role dtoToModel(RoleDTO dto, Role model) {
        if (model == null) {
            model = new Role();
        }
        model.setAbstract(dto);
        model.setName(dto.getName());
        model.setIsDefault(dto.getIsDefault());
        if (dto.getPermissions() != null) {
            model.setPermissions(permissionMapper.dtosToModels(dto.getPermissions()));
        }
        return model;
    }

    @Override
    public RoleDTO modelToPaginateDto(Role model) {
        RoleDTO dto = new RoleDTO();
        dto.setAbstract(model);
        dto.setName(model.getName());
        dto.setIsDefault(model.getIsDefault());
        return dto;
    }
}
