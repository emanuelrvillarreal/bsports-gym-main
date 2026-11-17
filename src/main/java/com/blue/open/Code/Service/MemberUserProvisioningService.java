package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberUserProvisioningService {

    private static final int STATUS_ACTIVE = MemberStatusConstants.STATUS_ACTIVE;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createPortalUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        validationHelper.validateAltaUserFields(fields);
        return createNewPortalUser(fields, company, roleEnum, false);
    }

    public User resolveOrCreatePortalUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        if (fields == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberUserProvisioningService | fields null");
        }
        if (fields.getEmail() == null || fields.getEmail().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email requerido",
                    "MemberUserProvisioningService | email vacío");
        }

        String email = fields.getEmail().trim();
        User existing = resolveExistingUser(email, fields.getDni());
        if (existing == null) {
            if (fields.getPassword() == null || fields.getPassword().isBlank()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Contraseña requerida",
                        "MemberUserProvisioningService | password requerido para usuario nuevo");
            }
            validationHelper.validateAltaUserFields(fields);
            return createNewPortalUser(fields, company, roleEnum, true);
        }

        Role role = findRoleOrThrow(company, roleEnum);
        User userWithDetails = userRepository.findFirstByUuidWithDetails(existing.getUuid());
        if (userWithDetails == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar usuario",
                    "MemberUserProvisioningService | userUuid: " + existing.getUuid());
        }
        ensurePortalRoleForCompany(userWithDetails, company, role);
        return userWithDetails;
    }

    public User resolveOrCreateMigrationUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        return resolveOrCreateMigrationUser(fields, company, findRoleOrThrow(company, roleEnum));
    }

    /**
     * Igual que resolveOrCreateMigrationUser(fields, company, RoleEnum), pero para roles que no
     * forman parte de RoleEnum (p.ej. ADMIN, que es un BaseRoleEnum resuelto por
     * ClubRoleProvisioningService.getAdminRole).
     */
    public User resolveOrCreateMigrationUser(MemberAltaUserFields fields, Company company, Role role) {
        if (fields == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberUserProvisioningService | fields null");
        }
        if (fields.getEmail() == null || fields.getEmail().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email requerido",
                    "MemberUserProvisioningService | email vacío");
        }

        String email = fields.getEmail().trim();
        User existing = resolveExistingUser(email, fields.getDni());

        // El email es único a nivel sistema (no por company): si ya existe un usuario
        // (activo o pendiente) lo reutilizamos para poder migrarlo a otro club, igual
        // que en el alta portal. El alta de socio en sí (MemberTitularService /
        // MemberFamiliarService) es quien bloquea si ya es socio de esta company.
        if (existing == null) {
            validationHelper.validateMigrationAltaFields(fields);
            return createNewPortalUser(fields, company, role, false);
        }

        User userWithDetails = userRepository.findFirstByUuidWithDetails(existing.getUuid());
        if (userWithDetails == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar usuario",
                    "MemberUserProvisioningService | userUuid: " + existing.getUuid());
        }
        ensurePortalRoleForCompany(userWithDetails, company, role);
        return userWithDetails;
    }

    /**
     * Busca un usuario existente que coincida con el email (=username) y/o dni informados.
     * Un usuario eliminado (soft-delete) nunca matchea acá: sus email/username/dni
     * quedan anonimizados con prefijo "deleted-{id}-" al momento del borrado
     * (ver MemberRegistroAprobacionService.anonymizeDeletedUser), así que no hace
     * falta contemplar su status en esta búsqueda.
     */
    private User resolveExistingUser(String email, String dni) {
        boolean hasDni = dni != null && !dni.isBlank();

        User byBoth = hasDni ? userRepository.findFirstByDniAndUsernameIgnoreCase(dni, email) : null;
        if (byBoth != null) {
            return byBoth;
        }

        User byUsername = userRepository.findFirstByUsernameIgnoringCase(email);
        if (byUsername != null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El correo electrónico ya está en uso por un usuario con un DNI distinto al ingresado.",
                    "MemberUserProvisioningService | resolveExistingUser email conflict, userUuid: "
                            + byUsername.getUuid());
        }

        if (hasDni) {
            User byDni = userRepository.findFirstByDni(dni);
            if (byDni != null) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "Ya existe un usuario registrado con ese DNI.",
                        "MemberUserProvisioningService | resolveExistingUser dni conflict, userUuid: "
                                + byDni.getUuid());
            }
        }

        return null;
    }

    private User createNewPortalUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum,
            boolean pendingApproval) {
        return createNewPortalUser(fields, company, findRoleOrThrow(company, roleEnum), pendingApproval);
    }

    private User createNewPortalUser(MemberAltaUserFields fields, Company company, Role role,
            boolean pendingApproval) {
        User user = new User();
        user.setStatus(pendingApproval ? MemberStatusConstants.STATUS_PENDING_APPROVAL : STATUS_ACTIVE);
        memberMapper.applyAltaUserFields(fields, user);
        user.setPassword(passwordEncoder.encode(fields.getPassword()));

        user = userRepository.save(user);
        attachUserCompanyRole(user, company, role);
        return userRepository.save(user);
    }

    private void ensurePortalRoleForCompany(User user, Company company, Role role) {
        UserCompanyRole existingUcr = findUserCompanyRole(user, company);
        if (existingUcr == null) {
            attachUserCompanyRole(user, company, role);
            userRepository.save(user);
            return;
        }

        boolean hasRole = existingUcr.getRoles() != null
                && existingUcr.getRoles().stream().anyMatch(r -> role.getName().equals(r.getName()));
        if (!hasRole) {
            if (existingUcr.getRoles() == null) {
                existingUcr.setRoles(new ArrayList<>());
            }
            existingUcr.getRoles().add(role);
            userCompanyRoleRepository.save(existingUcr);
        }
    }

    private UserCompanyRole findUserCompanyRole(User user, Company company) {
        if (user.getUserCompanyRoles() == null) {
            return null;
        }
        return user.getUserCompanyRoles().stream()
                .filter(ucr -> ucr.getCompany() != null && company.getId().equals(ucr.getCompany().getId()))
                .findFirst()
                .orElse(null);
    }

    private void attachUserCompanyRole(User user, Company company, Role role) {
        UserCompanyRole userCompanyRole = new UserCompanyRole();
        userCompanyRole.setUser(user);
        userCompanyRole.setCompany(company);
        userCompanyRole.setRoles(new ArrayList<>(List.of(role)));
        userCompanyRole = userCompanyRoleRepository.save(userCompanyRole);

        if (user.getUserCompanyRoles() == null) {
            user.setUserCompanyRoles(new ArrayList<>());
        }
        user.getUserCompanyRoles().add(userCompanyRole);
    }

    private Role findRoleOrThrow(Company company, RoleEnum roleEnum) {
        Role role = roleRepository.findFirstByNameAndCompany(roleEnum.name(), company);
        if (role == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Configuración incompleta",
                    "Rol " + roleEnum.name() + " no encontrado para el club.");
        }
        return role;
    }
}
