package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.RecepcionistaMapper;
import com.blue.open.Code.Repository.RecepcionistaRepository;
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
public class RecepcionistaUpdateService {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Autowired
    private RecepcionistaMapper recepcionistaMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public RecepcionistaDTO actualizar(String recepcionistaUuid, StaffProfileUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE, RoleEnum.ADM_RECEPCION));
        Company company = authService.getCurrentCompanyOrThrow();

        Recepcionista recepcionista = recepcionistaRepository
                .findByUuidAndCompanyIdWithUser(recepcionistaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado",
                        "RecepcionistaUpdateService | uuid: " + recepcionistaUuid));

        User user = recepcionista.getUser();
        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            validationHelper.validateEmailUnique(command.getEmail().trim(), user.getUuid());
        }
        validationHelper.validateDni(command.getDni());
        validationHelper.validateDniUnique(command.getDni(), user.getUuid());

        recepcionistaMapper.applyUpdate(command, recepcionista);
        userRepository.save(user);

        return recepcionistaMapper.modelToDto(
                recepcionistaRepository.findByIdWithUser(recepcionista.getId()).orElse(recepcionista));
    }
}
