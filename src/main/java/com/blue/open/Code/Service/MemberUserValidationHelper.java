package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.DTO.MemberBackofficeUpdateCommand;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MemberUserValidationHelper {

    private static final int STATUS_DELETED = -1;
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{7,11}$");

    @Autowired
    private UserRepository userRepository;

    public void validateAltaUserFields(MemberAltaUserFields fields) {
        if (fields == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberUserValidationHelper | command null");
        }
        if (fields.getNombre() == null || fields.getNombre().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Nombre requerido",
                    "MemberUserValidationHelper | nombre vacío");
        }
        if (fields.getApellido() == null || fields.getApellido().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Apellido requerido",
                    "MemberUserValidationHelper | apellido vacío");
        }
        if (fields.getEmail() == null || fields.getEmail().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email requerido",
                    "MemberUserValidationHelper | email vacío");
        }
        if (fields.getPassword() == null || fields.getPassword().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Contraseña requerida",
                    "MemberUserValidationHelper | password vacío");
        }
        validateDni(fields.getDni());
        validateDniUnique(fields.getDni(), null);
        validateEmailUnique(fields.getEmail().trim(), null);
    }

    public void validateEmailUnique(String email, String excludeUserUuid) {
        if (email == null || email.isBlank()) {
            return;
        }
        User existing = userRepository.findFirstByEmailAndStatusNot(email.trim(), STATUS_DELETED);
        if (existing != null && (excludeUserUuid == null || !existing.getUuid().equals(excludeUserUuid))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email ya registrado",
                    "El correo electrónico ya se encuentra en uso.");
        }
    }

    public void validateMigrationAltaFields(MemberAltaUserFields fields) {
        if (fields == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberUserValidationHelper | command null");
        }
        if (fields.getNombre() == null || fields.getNombre().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Nombre requerido",
                    "MemberUserValidationHelper | nombre vacío");
        }
        if (fields.getApellido() == null || fields.getApellido().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Apellido requerido",
                    "MemberUserValidationHelper | apellido vacío");
        }
        if (fields.getEmail() == null || fields.getEmail().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email requerido",
                    "MemberUserValidationHelper | email vacío");
        }
        if (fields.getPassword() == null || fields.getPassword().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Contraseña requerida",
                    "MemberUserValidationHelper | password vacío");
        }
        validateDni(fields.getDni());
        validateDniUnique(fields.getDni(), null);
    }

    public void validateDni(String dni) {
        if (dni != null && !dni.isBlank() && !DNI_PATTERN.matcher(dni.trim()).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "DNI inválido",
                    "El DNI debe contener entre 7 y 11 dígitos numéricos.");
        }
    }

    public void validateBackofficeUpdateEmail(MemberBackofficeUpdateCommand command, User user) {
        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            validateEmailUnique(command.getEmail().trim(), user.getUuid());
        }
        validateDni(command.getDni());
        validateDniUnique(command.getDni(), user.getUuid());
    }

    public void validateDniUnique(String dni, String excludeUserUuid) {
        if (dni == null || dni.isBlank()) {
            return;
        }
        User existing = userRepository.findFirstByDniAndStatusNot(dni.trim(), STATUS_DELETED);
        if (existing != null && (excludeUserUuid == null || !existing.getUuid().equals(excludeUserUuid))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "DNI ya registrado",
                    "Ya existe un usuario con ese DNI.");
        }
    }
}
