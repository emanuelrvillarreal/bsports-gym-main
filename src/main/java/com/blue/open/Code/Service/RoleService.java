package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Service.BaseRoleService;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RoleService extends BaseRoleService {

    @Override
    protected boolean canAssignRole(RoleDTO role, User currentUser, String companyUuid, Set<String> requesterPerms) {
        // Si el rol está en RoleEnum del proyecto, se permite la visibilidad/asignación
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.name().equals(role.getName())) {
                return true;
            }
        }

        // De lo contrario, aplicar la regla general del Core (debe tener todos los
        // permisos)
        return super.canAssignRole(role, currentUser, companyUuid, requesterPerms);
    }
}
