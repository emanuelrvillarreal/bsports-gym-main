package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AdminAltaCommand;
import com.blue.open.Code.Entity.DTO.AdminDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.AdminMapper;
import com.blue.open.Code.Service.ClubRoleProvisioningService;
import com.blue.open.Code.Service.MemberUserProvisioningService;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminAltaService {

    private static final String ROLE_NAME_ADMIN = "ADMIN";

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminCupoService adminCupoService;

    @Autowired
    private ClubRoleProvisioningService clubRoleProvisioningService;

    @Autowired
    private MemberUserProvisioningService userProvisioningService;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Transactional
    public AdminDTO alta(AdminAltaCommand command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        adminCupoService.validarCupoDisponible(company, 1);

        Role adminRole = clubRoleProvisioningService.getAdminRole(company);
        if (adminRole == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Configuración incompleta",
                    "AdminAltaService | rol ADMIN no encontrado para company " + company.getId());
        }
        User user = userProvisioningService.resolveOrCreateMigrationUser(command, company, adminRole);

        UserCompanyRole userCompanyRole = userCompanyRoleRepository
                .findByCompanyIdAndUserUuidAndRoleName(company.getId(), user.getUuid(), ROLE_NAME_ADMIN)
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear administrador",
                        "AdminAltaService | no se encontró la asignación ADMIN recién creada, userUuid: "
                                + user.getUuid()));
        return adminMapper.modelToDto(userCompanyRole);
    }
}
