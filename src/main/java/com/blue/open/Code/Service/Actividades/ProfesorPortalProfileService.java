package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberUserValidationHelper;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfesorPortalProfileService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorMapper profesorMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfesorDTO obtenerPerfil() {
        authService.valid();
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(company.getId()));
        return profesorMapper.modelToDto(profesor);
    }

    @Transactional
    public ProfesorDTO actualizarPerfil(StaffProfileUpdateCommand command) {
        authService.valid();
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(company.getId()));

        User user = profesor.getUser();
        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            validationHelper.validateEmailUnique(command.getEmail().trim(), user.getUuid());
        }
        validationHelper.validateDni(command.getDni());
        validationHelper.validateDniUnique(command.getDni(), user.getUuid());

        profesorMapper.applyUpdate(command, profesor);
        userRepository.save(user);

        return profesorMapper.modelToDto(
                profesorRepository.findByIdWithUser(profesor.getId()).orElse(profesor));
    }
}
