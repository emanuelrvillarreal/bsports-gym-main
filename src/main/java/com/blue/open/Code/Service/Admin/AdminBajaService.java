package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Baja logica de un ADMIN adicional: le remueve el rol "ADMIN" de su UserCompanyRole en este
 * club. Si esa asignacion se queda sin roles, se marca como eliminada (status = -1).
 * <p>
 * Nunca deja el club sin administradores: si es el ultimo, corta con 400.
 */
@Service
public class AdminBajaService {

    private static final int STATUS_DELETED = -1;
    private static final String ROLE_NAME_ADMIN = "ADMIN";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Transactional
    public void bajaLogica(String userUuid) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();

        List<UserCompanyRole> admins = userCompanyRoleRepository.findByCompanyAndRoleName(company, ROLE_NAME_ADMIN);
        if (admins.size() <= 1) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se puede eliminar el último administrador del gimnasio",
                    "AdminBajaService | companyId: " + company.getId() + " | userUuid: " + userUuid);
        }

        UserCompanyRole userCompanyRole = userCompanyRoleRepository
                .findByCompanyIdAndUserUuidAndRoleName(company.getId(), userUuid, ROLE_NAME_ADMIN)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Administrador no encontrado",
                        "AdminBajaService | userUuid: " + userUuid));

        userCompanyRole.getRoles().removeIf(role -> ROLE_NAME_ADMIN.equals(role.getName()));
        if (userCompanyRole.getRoles().isEmpty()) {
            userCompanyRole.setStatus(STATUS_DELETED);
        }
        userCompanyRoleRepository.save(userCompanyRole);
    }
}
