package com.blue.open.Core.Mapper;

import com.blue.open.Core.Entity.DTO.BaseUserDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseUserMapper<DTO extends BaseUserDTO>
        extends AbstractMapperComplete<User, DTO, DTO> {

    @Autowired
    protected UserCompanyRolesMapper userCompanyRolesMapper;

    @Override
    public DTO modelToDto(User model) {
        try {
            DTO dto = createDto();
            dto.setAbstract(model);
            dto.setUuid(model.getUuid());
            dto.setUsername(model.getUsername());
            dto.setEmail(model.getEmail());
            dto.setPhone(model.getPhone());

            if (model.getUserCompanyRoles() != null) {
                dto.setUserCompanyRoles(model.getUserCompanyRoles().stream()
                        .map(userCompanyRolesMapper::modelToDto)
                        .collect(Collectors.toList()));
            }

            return dto;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al convertir los datos",
                    "Error en el mapper modelToDto " + e.getMessage());
        }
    }

    @Override
    public User dtoToModel(DTO userDTO, User model) {
        if (model == null) {
            model = createModel();
        }
        model.setAbstract(userDTO);
        model.setUsername(userDTO.getUsername());
        model.setEmail(userDTO.getEmail());
        model.setPhone(userDTO.getPhone());
        model.setPassword(userDTO.getPassword());
        if (userDTO.getUserCompanyRoles() != null) {
            List<UserCompanyRole> roles = userCompanyRolesMapper.dtosToModels(userDTO.getUserCompanyRoles());
            if (roles != null) {
                model.getUserCompanyRoles().clear();
                for (UserCompanyRole r : roles) {
                    r.setUser(model);
                    model.getUserCompanyRoles().add(r);
                }
            }
        }

        return model;
    }

    @Override
    public DTO modelToPaginateDto(User model) {
        return modelToDto(model);
    }

    protected abstract DTO createDto();

    protected abstract User createModel();
}
