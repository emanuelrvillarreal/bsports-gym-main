package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.PaginateFiltro;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Auth.BasePermissionEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public class BaseRoleService extends AbstractService<Role, RoleDTO, RoleDTO> {

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PermissionRepository permissionRepository;

    @Override
    protected PaginateList hookPaginatePreQuery(PaginateList paginateList) {
        paginateList.init("auth_roles");
        User currentUser = authService.getActualOrThrow();
        boolean isGlobal = authService.isSysAdmin(currentUser);

        if (!isGlobal) {
            paginateList.addFiltro(List.of(
                    new PaginateFiltro("t", "company_id", "=",
                            authService.getCurrentCompanyOrThrow().getId().toString()),
                    new PaginateFiltro("t", "company_id", "=", "1")));
        }
        return paginateList;
    }

    @Override
    public void hookSaveAuth(RoleDTO request) {
        Company currentCompany = authService.getCurrentCompanyOrThrow();
        Role existing = roleRepository.findFirstByNameAndCompany(request.getName(), currentCompany);
        if (existing != null && (request.getUuid() == null || !existing.getUuid().equals(request.getUuid()))) {
            throw new CustomException("Ya existe un rol con ese nombre para su empresa");
        }

        User currentUser = authService.getActualOrThrow();
        String headerCompanyUuid = currentCompany.getUuid();

        boolean currentIsGlobal = authService.isSysAdmin(currentUser);

        boolean isCreate = (request.getUuid() == null);
        BasePermissionEnum requiredPerm = isCreate ? BasePermissionEnum.ROLE_CREATE : BasePermissionEnum.ROLE_EDIT;

        authService.valid(List.of(requiredPerm));

        String targetCompanyUuid = headerCompanyUuid;
        if (!isCreate) {
            Role existingRole = roleRepository.findFirstByUuid(request.getUuid());
            if (existingRole != null && existingRole.getCompany() != null) {
                targetCompanyUuid = existingRole.getCompany().getUuid();
            }
        }

        boolean isSystemRole = "1".equals(targetCompanyUuid);
        if (isSystemRole && !currentIsGlobal) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                    "Solo los administradores globales pueden gestionar roles de sistema.");
        }

        if (!currentIsGlobal && !headerCompanyUuid.equals(targetCompanyUuid)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                    "No tienes permisos para gestionar roles de otra empresa.");
        }

        if (!currentIsGlobal && request.getPermissions() != null) {
            Set<String> requesterPerms = authService.getAggregatedPermissions(currentUser, headerCompanyUuid);

            for (var pDto : request.getPermissions()) {
                String permName = pDto.getName();
                if (permName == null && pDto.getUuid() != null) {
                    Permission dbP = permissionRepository.findFirstByUuid(pDto.getUuid());
                    if (dbP != null)
                        permName = dbP.getName();
                }

                if (permName != null && !requesterPerms.contains(permName)) {
                    throw new CustomException(HttpStatus.FORBIDDEN,
                            "Escalación detectada",
                            "No puedes asignar el permiso '" + permName + "' porque tú mismo no lo posees.");
                }
            }
        }
    }

    @Override
    public void hookDeleteAuth(String uuid) {
        authService.valid(List.of(BasePermissionEnum.ROLE_DELETE));

        User currentUser = authService.getActualOrThrow();
        String headerCompanyUuid = authService.getCurrentCompanyOrThrow().getUuid();
        boolean currentIsGlobal = authService.isSysAdmin(currentUser);

        Role role = roleRepository.findFirstByUuid(uuid);
        if (role != null && role.getCompany() != null) {
            String targetCompanyUuid = role.getCompany().getUuid();
            boolean isSystemRole = "1".equals(targetCompanyUuid);

            if (isSystemRole && !currentIsGlobal) {
                throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                        "Solo los administradores globales pueden eliminar roles de sistema.");
            }

            if (!currentIsGlobal && !headerCompanyUuid.equals(targetCompanyUuid)) {
                throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                        "No tienes permisos para eliminar roles de otra empresa.");
            }
        }
    }

    @Override
    public void hookPaginateAuth(PaginateList paginateList) {
        authService.valid(List.of(BasePermissionEnum.ROLE_VIEW));
    }

    @Override
    protected List<RoleDTO> hookPaginateFilterResults(List<RoleDTO> dtoList) {
        User currentUser = authService.getActualOrThrow();
        if (authService.isSysAdmin(currentUser)) {
            return dtoList;
        }

        String companyUuid = authService.getCurrentCompanyOrThrow().getUuid();
        Set<String> requesterPerms = authService.getAggregatedPermissions(currentUser, companyUuid);

        return dtoList.stream().filter(role -> {
            return canAssignRole(role, currentUser, companyUuid, requesterPerms);
        }).collect(Collectors.toList());
    }

    protected boolean canAssignRole(RoleDTO role, User currentUser, String companyUuid, Set<String> requesterPerms) {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            return true;
        }
        return role.getPermissions().stream()
                .allMatch(p -> requesterPerms.contains(p.getName()));
    }

    @Override
    protected Role hookSaveMapperDtoToModel(RoleDTO dto, Role model) {
        model = super.hookSaveMapperDtoToModel(dto, model);
        if (dto.getPermissions() != null) {
            List<Permission> dbPermissions = new ArrayList<>();
            for (var pDto : dto.getPermissions()) {
                if (pDto.getUuid() != null) {
                    var p = permissionRepository.findFirstByUuid(pDto.getUuid());
                    if (p != null) {
                        dbPermissions.add(p);
                    }
                }
            }
            model.setPermissions(dbPermissions);
        }
        return model;
    }

    @Override
    protected Role hookSaveSetUserCreated(Role model) {
        model = super.hookSaveSetUserCreated(model);
        Company company = authService.getCurrentCompanyOrThrow();
        model.setCompany(company);
        return model;
    }

    @Override
    protected Role hookSaveDatabaseSave(Role model) {
        if (model.getIsDefault() != null && model.getIsDefault() == 1) {
            Company company = model.getCompany();
            if (company == null) {
                company = authService.getCurrentCompanyOrThrow();
            }
            List<Role> existingDefaults = roleRepository.findAllByCompanyAndIsDefault(company, 1);
            for (var role : existingDefaults) {
                if (!role.getUuid().equals(model.getUuid())) {
                    role.setIsDefault(0);
                    roleRepository.save(role);
                }
            }
        }
        return super.hookSaveDatabaseSave(model);
    }
}
