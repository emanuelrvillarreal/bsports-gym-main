package com.blue.open.Code.Validation;

import com.blue.open.Code.Entity.DTO.ClubAdminRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidClubRegistrationValidator implements ConstraintValidator<ValidClubRegistration, ClubRegistrationRequestDTO> {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(ClubRegistrationRequestDTO request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        ClubAdminRequestDTO admin = request.getAdmin();
        if (admin == null || admin.getEmail() == null || admin.getEmail().isBlank()
                || request.getEmailInstitucional() == null || request.getEmailInstitucional().isBlank()) {
            return true;
        }

        String adminEmail = admin.getEmail().trim();
        String institutionalEmail = request.getEmailInstitucional().trim();

        if (adminEmail.equalsIgnoreCase(institutionalEmail)) {
            addViolation(context, "admin.email",
                    "El email del administrador no puede ser igual al email institucional del club.");
            return false;
        }

        User existingUser = userRepository.findFirstByEmailAndStatusNot(adminEmail, STATUS_DELETED);
        if (existingUser != null) {
            addViolation(context, "admin.email",
                    "El correo electrónico del administrador ya se encuentra en uso.");
            return false;
        }

        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        var builder = context.buildConstraintViolationWithTemplate(message);
        if ("admin.email".equals(field)) {
            builder.addPropertyNode("admin").addPropertyNode("email").addConstraintViolation();
        } else {
            builder.addPropertyNode(field).addConstraintViolation();
        }
    }
}
