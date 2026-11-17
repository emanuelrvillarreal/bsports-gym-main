package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AdminDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.AdminMapper;
import com.blue.open.Code.Service.MemberUserValidationHelper;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUpdateService {

    private static final String ROLE_NAME_ADMIN = "ADMIN";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public AdminDTO actualizar(String userUuid, StaffProfileUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();

        UserCompanyRole userCompanyRole = resolveOrThrow(company.getId(), userUuid);
        User user = userCompanyRole.getUser();

        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            validationHelper.validateEmailUnique(command.getEmail().trim(), user.getUuid());
        }
        validationHelper.validateDni(command.getDni());
        validationHelper.validateDniUnique(command.getDni(), user.getUuid());

        adminMapper.applyUpdate(command, user);
        userRepository.save(user);

        return adminMapper.modelToDto(userCompanyRole);
    }

    private UserCompanyRole resolveOrThrow(Long companyId, String userUuid) {
        return userCompanyRoleRepository
                .findByCompanyIdAndUserUuidAndRoleName(companyId, userUuid, ROLE_NAME_ADMIN)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Administrador no encontrado",
                        "AdminUpdateService | userUuid: " + userUuid));
    }
}
