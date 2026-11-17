package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.RecepcionistaMapper;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Code.Service.MemberUserValidationHelper;
import com.blue.open.Code.Service.RecepcionistaAuthService;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecepcionistaPortalProfileService {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaAuthService recepcionistaAuthService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Autowired
    private RecepcionistaMapper recepcionistaMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public RecepcionistaDTO obtenerPerfil() {
        authService.valid();
        Company company = authService.getCurrentCompanyOrThrow();
        Recepcionista recepcionista = recepcionistaAuthService.requirePortalEligible(
                recepcionistaAuthService.getCurrentRecepcionistaOrThrow(company.getId()));
        return recepcionistaMapper.modelToDto(recepcionista);
    }

    @Transactional
    public RecepcionistaDTO actualizarPerfil(StaffProfileUpdateCommand command) {
        authService.valid();
        Company company = authService.getCurrentCompanyOrThrow();
        Recepcionista recepcionista = recepcionistaAuthService.requirePortalEligible(
                recepcionistaAuthService.getCurrentRecepcionistaOrThrow(company.getId()));

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
