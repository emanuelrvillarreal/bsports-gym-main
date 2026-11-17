package com.blue.open.Core.Service.Auth;

import com.blue.open.Core.Auth.IPermission;
import com.blue.open.Core.Auth.IRole;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.DTO.JwtTokenDTO;
import com.blue.open.Core.Entity.DTO.UserAndPasswordDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import com.blue.open.Core.Entity.Db.UserCompanyRole;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.blue.open.Core.Entity.Db.Permission;

@Service
public class AuthService {
    @Autowired
    AuthBasicService authBasicService;
    @Autowired
    AuthSsoService authSsoService;
    @Autowired
    AuthTokenService authTokenService;
    @Autowired
    AuthMalvinasService authMalvinasService;
    @Autowired
    AuthJwtService authJwtService;
    @Autowired
    BaseCompanyRepository companyRepository;
    @Autowired
    UserRepository userRepository;

    @Value("${auth.protocols}")
    private String protocols;

    @Value("${core.multi-company.enabled:true}")
    private boolean multiCompanyEnabled;

    public String resolveCompanyUuid(String companyUuid) {
        if ((companyUuid == null || companyUuid.trim().isEmpty()) && !multiCompanyEnabled) {
            return DatabaseInitializerService.DEFAULT_COMPANY_UUID;
        }
        return companyUuid;
    }

    public Company getDefaultCompany() {
        String defaultCompanyUuid = DatabaseInitializerService.DEFAULT_COMPANY_UUID;
        Company defaultCompany = companyRepository.findFirstByUuid(defaultCompanyUuid);
        if (defaultCompany == null) {
            try {
                Long companyId = Long.parseLong(defaultCompanyUuid);
                defaultCompany = companyRepository.findFirstById(companyId);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        if (defaultCompany == null) {
            defaultCompany = companyRepository.findFirstById(Long.parseLong(defaultCompanyUuid));
        }
        return defaultCompany;
    }

    private String getCompanyUuidFromRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes == null)
            return null;

        String companyUuid = requestAttributes.getRequest().getHeader("X-Company-Uuid");
        return resolveCompanyUuid(companyUuid);
    }

    public User getActualOrNull() {
        try {
            User user = resolveUserFromAuthProtocols();
            return loadUserWithAuthDetails(user);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el usuario",
                    "getActualOrNull: " + e.getMessage());
        }
    }

    private User resolveUserFromAuthProtocols() {
        if (validProtocolBoolean("jwt")) {
            User user = authJwtService.getUser();
            if (user != null)
                return user;
        }
        if (validProtocolBoolean("basic")) {
            User user = authBasicService.getUser();
            if (user != null)
                return user;
        }
        if (validProtocolBoolean("tokens")) {
            User user = authTokenService.getUser();
            if (user != null)
                return user;
        }
        if (validProtocolBoolean("malvinas")) {
            User user = authMalvinasService.getUser();
            if (user != null)
                return user;
        }
        if (validProtocolBoolean("sso")) {
            User user = authSsoService.getUser();
            if (user != null)
                return user;
        }
        return null;
    }

    private User loadUserWithAuthDetails(User user) {
        if (user == null) {
            return null;
        }
        return userRepository.findFirstByUuidWithDetails(user.getUuid());
    }

    public User getActualOrThrow() {
        User user = getActualOrNull();
        if (user == null)
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "Usuario no identificado",
                    "No se encontró usuario logeado");
        return user;
    }

    public User valid() {
        User user = getActualOrThrow();

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes != null) {
            String companyUuid = getCompanyUuidFromRequest();
            if (companyUuid != null && !companyUuid.trim().isEmpty()) {
                boolean belongsToCompany = user.getUserCompanyRoles().stream()
                        .anyMatch(ucr -> ucr.getCompany() != null && companyUuid.equals(ucr.getCompany().getUuid()));

                if (!belongsToCompany && !isSysAdmin(user)) {
                    throw new CustomException(HttpStatus.FORBIDDEN, "Empresa no permitida",
                            "El usuario no pertenece a la compañía o no está validado");
                }
            }
        }
        return user;
    }

    public User valid(List<Enum<?>> required) {
        User user = valid();

        if (!hasAnyRoleOrPermission(user, required)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso",
                    "Falta de permisos para acceder a la ruta");
        }
        return user;
    }

    public Company getCurrentCompanyOrThrow() {
        User user = valid();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes == null)
            throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida",
                    "No se encontró contexto de peticion");
        String companyUuid = getCompanyUuidFromRequest();
        if (companyUuid == null || companyUuid.trim().isEmpty())
            throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida", "Falta el header X-Company-Uuid");

        UserCompanyRole activeCompanyRole = user.getUserCompanyRoles().stream()
                .filter(ucr -> ucr.getCompany() != null && companyUuid.equals(ucr.getCompany().getUuid()))
                .findFirst()
                .orElse(null);

        if (activeCompanyRole != null) {
            return activeCompanyRole.getCompany();
        }

        if (isSysAdmin(user)) {
            Company company = companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
            if (company == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Company no encontrada",
                        "El UUID suministrado no es válido");
            }
            return company;
        }

        throw new CustomException(HttpStatus.FORBIDDEN, "No permitido", "No tienes acceso a esta empresa.");
    }

    /**
     * Valida la sesión actual y solicita la creación de un nuevo token de acceso.
     *
     * @param name Nombre identificador del token
     * @return UserAccessToken generado
     */
    public UserAccessToken createAccessToken(String name) {
        return authTokenService.createAccessToken(valid(), name);
    }

    /**
     * Valida la sesión actual y solicita el borrado lógico de un token.
     *
     * @param uuid UUID del token a eliminar
     * @return UserAccessToken eliminado
     */
    public UserAccessToken deleteAccessToken(String uuid) {
        return authTokenService.deleteAccessToken(valid(), uuid);
    }

    /**
     * Valida la sesión actual y obtiene todos los tokens del usuario.
     *
     * @return Lista de UserAccessToken
     */
    public List<UserAccessToken> getMyTokens() {
        return authTokenService.getTokensByUser(valid());
    }

    public JwtTokenDTO generateJwtToken(User user) {
        return authJwtService.generateToken(user);
    }

    public JwtTokenDTO refreshJwtToken(String refreshToken) {
        return authJwtService.refreshToken(refreshToken);
    }

    public boolean validBoolean(List<Enum<?>> required) {
        User user = getActualOrNull();
        if (user == null)
            throw new CustomException(HttpStatus.FORBIDDEN, "Usuario no identificado",
                    "No se encontró usuario logeado");

        return hasAnyRoleOrPermission(user, required);
    }

    public boolean hasAnyRoleOrPermission(User user, List<Enum<?>> required) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes == null)
            return false;
        String companyUuid = getCompanyUuidFromRequest();

        List<Role> mergedRoles = new java.util.ArrayList<>();
        if (user.getUserCompanyRoles() != null) {
            for (UserCompanyRole ucr : user.getUserCompanyRoles()) {
                if (ucr.getCompany() != null && ucr.getRoles() != null) {
                    if (isSystemCompany(ucr.getCompany())
                            || (companyUuid != null && companyUuid.equals(ucr.getCompany().getUuid()))) {
                        mergedRoles.addAll(ucr.getRoles());
                    }
                }
            }
        }

        if (mergedRoles.isEmpty())
            return false;

        return required.stream().anyMatch(req -> {
            if (req instanceof IRole) {
                String roleName = ((IRole) req).name();
                return mergedRoles.stream().anyMatch(userRole -> userRole.getName().equals(roleName));
            }
            if (req instanceof IPermission) {
                String permissionName = ((IPermission) req).getName();
                return mergedRoles.stream()
                        .flatMap(userRole -> userRole.getPermissions() != null ? userRole.getPermissions().stream()
                                : Stream.empty())
                        .anyMatch(userPermission -> userPermission.getName().equals(permissionName));
            }
            return false;
        });
    }

    public boolean validProtocolBoolean(String protocol) {
        List<String> protocolList = List.of(protocols.split(","));
        return protocolList.contains(protocol);
    }

    public User getUserAndPassword(UserAndPasswordDTO body) {
        return authJwtService.getUserAndPassword(body);
    }

    public boolean isSysAdmin() {
        return isSysAdmin(getActualOrNull());
    }

    public boolean isSysAdmin(User user) {
        if (user == null || user.getUserCompanyRoles() == null)
            return false;
        return user.getUserCompanyRoles().stream()
                .anyMatch(ucr -> ucr.getCompany() != null && isSystemCompany(ucr.getCompany()));
    }

    public boolean isAdmin(User user, String companyUuid) {
        if (user == null || user.getUserCompanyRoles() == null || companyUuid == null)
            return false;

        if (isSysAdmin(user))
            return true;

        return user.getUserCompanyRoles().stream()
                .filter(ucr -> ucr.getCompany() != null && companyUuid.equals(ucr.getCompany().getUuid()))
                .flatMap(ucr -> ucr.getRoles().stream())
                .anyMatch(r -> "ADMIN".equals(r.getName()));
    }

    public boolean isAdmin() {
        User user = getActualOrNull();
        if (user == null)
            return false;

        String companyUuid = getCompanyUuidFromRequest();
        if (companyUuid == null || companyUuid.trim().isEmpty())
            return isSysAdmin(user);

        return isAdmin(user, companyUuid);
    }

    public boolean isAdmin(String companyUuid) {
        return isAdmin(getActualOrNull(), companyUuid);
    }

    public Set<String> getAggregatedPermissions(User user, String companyUuid) {
        if (user == null || user.getUserCompanyRoles() == null || companyUuid == null)
            return Collections.emptySet();

        return user.getUserCompanyRoles().stream()
                .filter(ucr -> ucr.getCompany() != null &&
                        (isSystemCompany(ucr.getCompany()) || companyUuid.equals(ucr.getCompany().getUuid())))
                .flatMap(ucr -> ucr.getRoles().stream())
                .filter(Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    private boolean isSystemCompany(Company company) {
        if (company == null)
            return false;
        return Long.valueOf(1).equals(company.getId()) || "1".equals(company.getUuid());
    }
}
