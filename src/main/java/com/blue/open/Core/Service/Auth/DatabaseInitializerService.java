package com.blue.open.Core.Service.Auth;

import com.blue.open.Core.Auth.IPermission;
import com.blue.open.Core.Auth.IRole;
import com.blue.open.Core.Auth.BasePermissionEnum;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Code.Service.ClubRoleProvisioningService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Order(10)
@Service
public class DatabaseInitializerService {

    private static final String SYSTEM_COMPANY_UUID = "1";
    public static final String DEFAULT_COMPANY_UUID = "2";

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BaseCompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClubRoleProvisioningService clubRoleProvisioningService;

    @Value("${auth.rbac.sync-roles-on-startup:true}")
    private boolean syncRolesOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("Iniciando inicialización de Base de Datos (Permisos, Empresas, Roles, Usuarios)...");

        syncPermissions(BasePermissionEnum.values());
        syncProjectEnums("com.blue.open.Code.Auth.PermissionEnum", this::syncPermissionsFromGeneric);

        Company systemCompany = syncCompanies();

        syncRoles(systemCompany, BaseRoleEnum.values());

        if (syncRolesOnStartup) {
            log.info("Sincronizando roles plantilla en todas las empresas activas (auth.rbac.sync-roles-on-startup=true)");
            clubRoleProvisioningService.syncAllActiveCompanies();
        } else {
            log.info("Omitida sincronización global de roles (auth.rbac.sync-roles-on-startup=false)");
        }

        syncInitialUser(systemCompany);

        log.info("Inicialización de Base de Datos finalizada.");
    }

    private void syncProjectEnums(String className, Consumer<Object[]> syncMethod) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum()) {
                Object[] values = (Object[]) clazz.getMethod("values").invoke(null);
                log.info("Detectados Enums de proyecto para sincronización: {}", className);
                syncMethod.accept(values);
            }
        } catch (ClassNotFoundException e) {
            log.debug("No se detectaron Enums de proyecto en: {}", className);
        } catch (Exception e) {
            log.error("Error al intentar sincronizar Enums de proyecto {}: {}", className, e.getMessage());
        }
    }

    private void syncPermissionsFromGeneric(Object[] genericPermissions) {
        IPermission[] permissions = new IPermission[genericPermissions.length];
        for (int i = 0; i < genericPermissions.length; i++) {
            permissions[i] = (IPermission) genericPermissions[i];
        }
        syncPermissions(permissions);
    }

    private void syncPermissions(IPermission[] permissions) {
        for (IPermission permEnum : permissions) {
            Permission permission = permissionRepository.findFirstByName(permEnum.getName());
            if (permission == null) {
                permission = new Permission();
                permission.setName(permEnum.getName());
                permissionRepository.save(permission);
                log.info("Permiso sincronizado: {}", permEnum.getName());
            }
        }
    }

    /**
     * Empresa sistema (uuid=1): roles globales y SysAdmin.
     * Empresa operativa por defecto (uuid=2): company BlueOpenData.
     */
    private Company syncCompanies() {
        ensureCompany(SYSTEM_COMPANY_UUID, "Sistema", 1L);
        ensureCompany(DEFAULT_COMPANY_UUID, "BlueOpenData", null);
        return companyRepository.findFirstByUuid(SYSTEM_COMPANY_UUID);
    }

    private Company ensureCompany(String uuid, String name, Long preferredId) {
        Company company = companyRepository.findFirstByUuid(uuid);
        if (company == null) {
            company = new Company();
            company.setName(name);
            company.setStatus(0);
            company.setUuid(uuid);
            company.setPath("club-" + uuid);
            if (preferredId != null) {
                company.setId(preferredId);
            }
            company = companyRepository.save(company);
            log.info("Empresa creada: {} (uuid={})", name, uuid);
        } else if (!uuid.equals(company.getUuid())) {
            company.setUuid(uuid);
            company = companyRepository.save(company);
            log.info("Empresa actualizada a uuid={}: {}", uuid, name);
        }
        return company;
    }

    private void syncRoles(Company systemCompany, IRole[] roles) {
        for (IRole roleEnum : roles) {
            Role role = roleRepository.findFirstByNameAndCompany(roleEnum.name(), systemCompany);

            if (role == null) {
                role = new Role();
                role.setName(roleEnum.name());
                role.setCompany(systemCompany);
                log.info("Creando rol: {}", roleEnum.name());
            }

            Set<? extends IPermission> enumPermissions = roleEnum.getPermissions();
            List<Permission> dbPermissions = new ArrayList<>();

            if (enumPermissions != null) {
                Set<String> seenPermissionNames = new LinkedHashSet<>();
                for (IPermission permEnum : enumPermissions) {
                    if (!seenPermissionNames.add(permEnum.getName())) {
                        continue;
                    }
                    Permission permission = permissionRepository.findFirstByName(permEnum.getName());
                    if (permission != null) {
                        dbPermissions.add(permission);
                    } else {
                        log.warn("Permiso {} no encontrado para el rol {}", permEnum.getName(), roleEnum.name());
                    }
                }
            }

            role.setPermissions(dbPermissions);
            roleRepository.save(role);
            log.info("Rol sincronizado: {}", roleEnum.name());
        }
    }

    private void syncInitialUser(Company systemCompany) {
        User user = userRepository.findFirstByUsernameAndStatus("sysadmin", 0);
        if (user == null) {
            user = new User();
            user.setName("SysAdmin");
            user.setUsername("sysadmin");
            user.setPassword(passwordEncoder.encode("123"));
            user.setStatus(0);
            user = userRepository.save(user);
            log.info("Usuario inicial creado: SysAdmin");
        }

        Role sysAdminRole = roleRepository.findFirstByNameAndCompany(BaseRoleEnum.SYSADMIN.name(), systemCompany);
        if (sysAdminRole != null) {
            boolean alreadyLinked = user.getUserCompanyRoles() != null && user.getUserCompanyRoles().stream()
                    .anyMatch(
                            ucr -> ucr.getCompany() != null && ucr.getCompany().getId().equals(systemCompany.getId()));

            if (!alreadyLinked) {
                UserCompanyRole userCompanyRole = new UserCompanyRole();
                userCompanyRole.setUser(user);
                userCompanyRole.setCompany(systemCompany);
                userCompanyRole.setRoles(new ArrayList<>(Collections.singletonList(sysAdminRole)));

                userCompanyRole = userCompanyRoleRepository.save(userCompanyRole);

                if (user.getUserCompanyRoles() == null) {
                    user.setUserCompanyRoles(new ArrayList<>());
                }
                user.getUserCompanyRoles().add(userCompanyRole);
                userRepository.save(user);

                log.info("Usuario SysAdmin vinculado a empresa sistema (uuid={}) con rol SYSADMIN", SYSTEM_COMPANY_UUID);
            }
        } else {
            log.error("FALTA CRÍTICA: El rol SYSADMIN no se encontró tras la sincronización.");
        }
    }
}
