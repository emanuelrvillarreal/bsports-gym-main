package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Auth.IPermission;
import com.blue.open.Core.Auth.IRole;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ClubRoleProvisioningService {

    private static final int STATUS_ACTIVE = 0;
    private static final String ADMIN_ROLE_NAME = BaseRoleEnum.ADMIN.name();
    private static final String SYNC_MODE_ADDITIVE = "additive";

    private static final Set<String> TEMPLATE_ROLE_NAMES = Stream.concat(
            Stream.of(ADMIN_ROLE_NAME),
            Stream.concat(
                    Arrays.stream(RoleEnum.values()).map(Enum::name),
                    Arrays.stream(BaseRoleEnum.values()).map(Enum::name)))
            .collect(Collectors.toUnmodifiableSet());

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Value("${auth.rbac.role-sync-mode:replace}")
    private String roleSyncMode;

    public static boolean isTemplateRoleName(String roleName) {
        return roleName != null && TEMPLATE_ROLE_NAMES.contains(roleName);
    }

    @Transactional
    public void syncAllActiveCompanies() {
        List<Company> companies = companyRepository.findByStatus(STATUS_ACTIVE);
        log.info("Sincronizando roles plantilla en {} empresas activas", companies.size());
        for (Company company : companies) {
            provisionRoles(company);
        }
    }

    @Transactional
    public void provisionRoles(Company company) {
        syncRole(company, ADMIN_ROLE_NAME, BaseRoleEnum.ADMIN);
        for (RoleEnum roleEnum : RoleEnum.values()) {
            syncRole(company, roleEnum.name(), roleEnum);
        }
        log.info("Roles plantilla sincronizados para empresa uuid={} ({} roles)", company.getUuid(),
                RoleEnum.values().length + 1);
    }

    public Role getAdminRole(Company company) {
        return roleRepository.findFirstByNameAndCompany(ADMIN_ROLE_NAME, company);
    }

    private void syncRole(Company company, String roleName, IRole roleEnum) {
        if (!isTemplateRoleName(roleName)) {
            return;
        }

        Role role = roleRepository.findFirstByNameAndCompany(roleName, company);

        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role.setCompany(company);
            role.setIsDefault(0);
            log.info("Creando rol {} para empresa uuid={}", roleName, company.getUuid());
        }

        List<Permission> resolved = resolvePermissions(roleEnum.getPermissions());
        if (SYNC_MODE_ADDITIVE.equalsIgnoreCase(roleSyncMode)) {
            role.setPermissions(mergePermissions(role.getPermissions(), resolved));
        } else {
            role.setPermissions(resolved);
        }
        roleRepository.save(role);
        log.debug("Rol {} sincronizado para empresa uuid={}", roleName, company.getUuid());
    }

    private List<Permission> mergePermissions(List<Permission> existing, List<Permission> fromEnum) {
        List<Permission> merged = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();
        if (existing != null) {
            for (Permission p : existing) {
                if (p != null && p.getId() != null && seenIds.add(p.getId())) {
                    merged.add(p);
                }
            }
        }
        for (Permission p : fromEnum) {
            if (p != null && p.getId() != null && seenIds.add(p.getId())) {
                merged.add(p);
            }
        }
        return merged;
    }

    private List<Permission> resolvePermissions(Set<? extends IPermission> enumPermissions) {
        List<Permission> dbPermissions = new ArrayList<>();
        if (enumPermissions == null) {
            return dbPermissions;
        }

        Set<String> seenPermissionNames = new LinkedHashSet<>();
        for (IPermission permEnum : enumPermissions) {
            if (!seenPermissionNames.add(permEnum.getName())) {
                continue;
            }
            Permission permission = permissionRepository.findFirstByName(permEnum.getName());
            if (permission != null) {
                dbPermissions.add(permission);
            } else {
                log.warn("Permiso {} no encontrado en BD", permEnum.getName());
            }
        }
        return dbPermissions;
    }
}
