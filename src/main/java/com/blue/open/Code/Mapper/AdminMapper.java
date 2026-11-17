package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.AdminDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import org.springframework.stereotype.Service;

@Service
public class AdminMapper {

    public AdminDTO modelToDto(UserCompanyRole userCompanyRole) {
        if (userCompanyRole == null) {
            return null;
        }
        User user = userCompanyRole.getUser();
        if (user == null) {
            return null;
        }
        AdminDTO dto = new AdminDTO();
        dto.setUserUuid(user.getUuid());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setDni(user.getDni());
        dto.setSexo(user.getSexo());
        dto.setTelefono(user.getPhone());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public void applyUpdate(StaffProfileUpdateCommand command, User user) {
        if (command.getNombre() != null) {
            user.setNombre(trimOrNull(command.getNombre()));
        }
        if (command.getApellido() != null) {
            user.setApellido(trimOrNull(command.getApellido()));
        }
        if (command.getDni() != null) {
            user.setDni(trimOrNull(command.getDni()));
        }
        if (command.getFechaNacimiento() != null) {
            user.setFechaNacimiento(command.getFechaNacimiento());
        }
        if (command.getSexo() != null) {
            user.setSexo(command.getSexo());
        }
        if (command.getTelefono() != null) {
            user.setPhone(command.getTelefono().trim());
        }
        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            String email = command.getEmail().trim();
            user.setEmail(email);
            user.setUsername(email);
        }
        syncDisplayName(user);
    }

    private void syncDisplayName(User user) {
        String nombre = user.getNombre();
        String apellido = user.getApellido();
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
        user.setName(fullName.toString());
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
