package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Core.Mapper.BaseUserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserMapper extends BaseUserMapper<UserDTO> {

    @Override
    public UserDTO modelToDto(User model) {
        UserDTO dto = super.modelToDto(model);
        dto.setNombre(model.getNombre());
        dto.setApellido(model.getApellido());
        dto.setDni(model.getDni());
        dto.setFechaNacimiento(model.getFechaNacimiento());
        dto.setImageUrl(model.getImageUrl());
        dto.setTelefono(model.getPhone());
        return dto;
    }

    @Override
    public User dtoToModel(UserDTO dto, User model) {
        model = super.dtoToModel(dto, model);
        model.setNombre(dto.getNombre());
        model.setApellido(dto.getApellido());
        model.setDni(dto.getDni());
        model.setFechaNacimiento(dto.getFechaNacimiento());
        model.setImageUrl(dto.getImageUrl());

        String telefono = dto.getTelefono();
        if (telefono != null) {
            model.setPhone(telefono);
        } else if (dto.getPhone() != null) {
            model.setPhone(dto.getPhone());
        }

        syncDisplayName(model);
        return model;
    }

    private void syncDisplayName(User model) {
        String nombre = model.getNombre();
        String apellido = model.getApellido();
        if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank())) {
            return;
        }
        StringBuilder fullName = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) {
            fullName.append(nombre.trim());
        }
        if (apellido != null && !apellido.isBlank()) {
            if (fullName.length() > 0) {
                fullName.append(' ');
            }
            fullName.append(apellido.trim());
        }
        model.setName(fullName.toString());
    }

    @Override
    protected UserDTO createDto() {
        return new UserDTO();
    }

    @Override
    protected User createModel() {
        return new User();
    }
}
