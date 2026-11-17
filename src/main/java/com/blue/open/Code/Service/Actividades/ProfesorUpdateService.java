package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberUserValidationHelper;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfesorUpdateService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorMapper profesorMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ProfesorDTO actualizar(String profesorUuid, StaffProfileUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE, RoleEnum.ADM_RECEPCION));
        Company company = authService.getCurrentCompanyOrThrow();

        Profesor profesor = profesorRepository
                .findByUuidAndCompanyIdWithUser(profesorUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Profesor no encontrado",
                        "ProfesorUpdateService | uuid: " + profesorUuid));

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
