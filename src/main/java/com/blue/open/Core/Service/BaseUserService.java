package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Notifications.MailNotification;
import com.blue.open.Core.Auth.BasePermissionEnum;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.PaginateFiltro;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.DTO.PaginateRelacion;
import com.blue.open.Core.Entity.DTO.PublicRegisterRequest;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.DTO.BaseUserDTO;
import com.blue.open.Core.Entity.DTO.ChangePasswordDTO;
import com.blue.open.Core.Entity.DTO.UserCompanyRoleDTO;
import com.blue.open.Core.Entity.DTO.UserAccessTokenDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Mapper.UserAccessTokenMapper;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Entity.Db.SecurityCode;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import com.blue.open.Core.Repository.SecurityCodeRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Transactional
public class BaseUserService<DTO extends BaseUserDTO> extends AbstractService<User, DTO, DTO> {
    public static final String PURPOSE_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String PURPOSE_REGISTRATION = "REGISTRATION";

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected BaseCompanyRepository companyRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected JavaMailSender javaMailSender;

    @Autowired
    protected BaseCompanyService<CompanyDTO> companyService;

    @Autowired
    protected SecurityCodeRepository securityCodeRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserAccessTokenMapper userAccessTokenMapper;

    @Override
    protected User hookUuidFindDatabase(String uuid) {
        return userRepository.findFirstByUuidWithDetails(uuid);
    }

    @Override
    public void hookSaveAuth(DTO request) {
        User currentUser = (User) authService.getActualOrThrow();
        String headerCompanyUuid = authService.getCurrentCompanyOrThrow().getUuid();

        boolean currentIsGlobal = authService.isSysAdmin(currentUser);
        boolean currentIsLocal = authService.isAdmin(currentUser, headerCompanyUuid);

        boolean isCreate = (request.getUuid() == null);
        BasePermissionEnum requiredPerm = isCreate ? BasePermissionEnum.USER_CREATE : BasePermissionEnum.USER_EDIT;

        authService.valid(List.of(requiredPerm));

        if (!currentIsGlobal) {
            // 1. Si es edición, validar que el usuario a editar pertenezca a la empresa del
            // admin de cabecera
            if (!isCreate) {
                DTO existing = getByUuid(request.getUuid());
                boolean belongsToCompany = existing.getUserCompanyRoles().stream()
                        .anyMatch(ucr -> headerCompanyUuid.equals(ucr.getCompanyUuid()));
                if (!belongsToCompany) {
                    throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                            "No puedes editar un usuario que no pertenece a tu empresa.");
                }
            }

            // 2. Validar que todos los roles asignados en el request sean para la empresa
            // del admin
            if (request.getUserCompanyRoles() != null) {
                for (var ucrDto : request.getUserCompanyRoles()) {
                    // Validar la empresa de la relación
                    if (ucrDto.getCompanyUuid() != null && !headerCompanyUuid.equals(ucrDto.getCompanyUuid())) {
                        throw new CustomException(HttpStatus.FORBIDDEN, "No permitido",
                                "No puedes crear/editar usuarios para una empresa distinta a la tuya.");
                    }

                    // Validar que los roles mismos pertenezcan a la empresa permitida
                    // (especialmente contra SYSADMIN)
                    if (ucrDto.getRoles() != null) {
                        for (var rDto : ucrDto.getRoles()) {
                            Role dbRole = roleRepository.findFirstByUuid(rDto.getUuid());
                            if (dbRole != null) {
                                boolean isSysRole = dbRole.getCompany() != null
                                        && (Long.valueOf(1).equals(dbRole.getCompany().getId())
                                                || "1".equals(dbRole.getCompany().getUuid()));
                                if (isSysRole) {
                                    // Bypassing check if hook specifically allows it
                                    if (!hookCanAssignRole(dbRole, currentUser, headerCompanyUuid)) {
                                        throw new CustomException(HttpStatus.FORBIDDEN,
                                                "No permitido",
                                                "No tienes permisos para asignar roles de sistema o de administrador global.");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Validación de escalación de permisos y asignación de rol ADMIN
        if (!currentIsGlobal && request.getUserCompanyRoles() != null) {
            Set<String> requesterPerms = authService.getAggregatedPermissions(currentUser, headerCompanyUuid);

            for (var ucrDto : request.getUserCompanyRoles()) {
                if (ucrDto.getRoles() != null) {
                    for (var rDto : ucrDto.getRoles()) {
                        Role dbRole = roleRepository.findFirstByUuid(rDto.getUuid());
                        if (dbRole != null) {
                            // Si es un admin local intentando asignar rol ADMIN de su propia empresa
                            if ("ADMIN".equals(dbRole.getName()) && !currentIsLocal) {
                                throw new CustomException(HttpStatus.FORBIDDEN,
                                        "No permitido",
                                        "No puedes asignar el rol 'ADMIN' si tú mismo no eres administrador de la empresa.");
                            }

                            // Verificar que el rol sea permitible (Hook de proyecto)
                            if (!hookCanAssignRole(dbRole, currentUser, headerCompanyUuid)) {
                                throw new CustomException(HttpStatus.FORBIDDEN, "Rol no permitido",
                                        "No tienes permiso para otorgar el rol: " + dbRole.getName());
                            }

                            // Verificar que no esté otorgando permisos que él mismo no tiene
                            for (var perm : dbRole.getPermissions()) {
                                if (!requesterPerms.contains(perm.getName())) {
                                    throw new CustomException(HttpStatus.FORBIDDEN,
                                            "Escalación detectada",
                                            "No puedes otorgar el permiso '" + perm.getName() + "' incluido en el rol '"
                                                    + dbRole.getName() + "' porque tú mismo no lo posees en la empresa "
                                                    + headerCompanyUuid);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void hookDeleteAuth(String uuid) {
        authService.valid(List.of(BasePermissionEnum.USER_DELETE));
    }

    @Override
    public void hookPaginateAuth(PaginateList paginateList) {
        authService.valid(List.of(BasePermissionEnum.USER_VIEW));
    }

    @Override
    protected PaginateList hookPaginatePreQuery(PaginateList paginateList) {
        paginateList.init("auth_users");
        User currentUser = (User) authService.getActualOrThrow();
        boolean isGlobal = authService.isSysAdmin(currentUser);

        if (!isGlobal) {
            paginateList.getRelaciones().add(new PaginateRelacion("inner", "auth_users_companies", "user_id", "t", "id"));
            paginateList.addFiltro(List.of(new PaginateFiltro("auth_users_companies", "company_id", "=",
                    authService.getCurrentCompanyOrThrow().getId().toString())));
        }
        return paginateList;
    }

    @Override
    protected User hookSaveMapperDtoToModel(DTO dto, User model) {
        model = super.hookSaveMapperDtoToModel(dto, model);

        // Encode password if provided (and raw)
        if (dto.getPassword() != null && !dto.getPassword().isEmpty() && !dto.getPassword().startsWith("$2a$")) {
            model.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return model;
    }

    @Override
    protected User hookSaveSetUserCreated(User model) {
        super.hookSaveSetUserCreated(model);
        return model;
    }

    public DTO publicRegister(PublicRegisterRequest<DTO, CompanyDTO> request) {
        DTO userDTO = request.getUser();
        CompanyDTO companyDTO = request.getCompany();

        if (companyDTO == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos incompletos",
                    "La compañía no puede ser nula al registrarse. Proporcione los datos de la compañía.");
        }

        if (companyDTO.getCuit() != null) {
            Company existingCompany = companyRepository.findFirstByCuit(companyDTO.getCuit());
            if (existingCompany != null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Empresa ya existe",
                        "Ya existe una empresa registrada con el CUIT: " + companyDTO.getCuit());
            }
        }

        // Hook para validar datos previo a creación de la empresa
        hookPublicRegisterPreCreateCompany(userDTO, companyDTO);

        CompanyDTO newCompanyDto = companyService.saveOrCreate(companyDTO);

        Role managerRole = roleRepository.findFirstByName(BaseRoleEnum.ADMIN.name());
        if (managerRole == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Configuración incompleta",
                    "Rol ADMIN no encontrado. Contacte al administrador del sistema.");
        }

        UserCompanyRoleDTO ucrDTO = new UserCompanyRoleDTO();
        ucrDTO.setCompanyUuid(newCompanyDto.getUuid());

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setUuid(managerRole.getUuid());
        roleDTO.setName(managerRole.getName());

        ucrDTO.setRoles(List.of(roleDTO));
        userDTO.setUserCompanyRoles(List.of(ucrDTO));

        // Encriptar contrasena
        if (userDTO.getPassword() != null) {
            userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Bypass saveOrCreate para evitar hookSaveAuth
        User user = (User) this.mapper.dtoToModel(userDTO, new User());
        user.setStatus(-2);
        generateAndSetVerificationCode(user);

        // Llamamos al hook brindando al usuario (no guardado aún) y a la entidad
        // Company
        Company newCompany = companyRepository.findFirstByUuid(newCompanyDto.getUuid());
        user = hookPublicRegisterPreSave(user, newCompany);

        user = (User) this.repository.save(user);

        sendVerificationEmail(user, null);

        return this.mapper.modelToDto(user);
    }

    public DTO publicLink(String companyUuid, String userUuid) {
        companyUuid = authService.resolveCompanyUuid(companyUuid);
        // Validar Empresa
        Company company = companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Empresa no encontrada",
                    "Company not found for uuid: " + companyUuid);
        }

        // Validar/Obtener Rol Default de la Empresa
        Role role = roleRepository.findFirstByCompanyAndIsDefault(company, 1);
        if (role == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Configuración incompleta",
                    "La empresa no tiene un rol por defecto configurado");
        }

        // Asignar Rol y Empresa al DTO
        UserCompanyRoleDTO ucrDTO = new UserCompanyRoleDTO();
        ucrDTO.setCompanyUuid(company.getUuid());

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setUuid(role.getUuid());
        roleDTO.setName(role.getName());

        DTO userDTO = getByUuid(userUuid);
        if (userDTO == null) {
            throw new CustomException("Usuario no encontrado", "User not found for uuid: " + userUuid);
        }

        ucrDTO.setRoles(List.of(roleDTO));
        userDTO.setUserCompanyRoles(List.of(ucrDTO));

        DTO savedDTO = this.saveOrCreate(userDTO);

        return savedDTO;
    }

    public DTO publicRegisterClient(String companyUuid, DTO userDTO) {
        companyUuid = authService.resolveCompanyUuid(companyUuid);
        // 1. Validar Empresa
        Company company = companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Empresa no encontrada",
                    "Company not found for uuid: " + companyUuid);
        }

        // 2. Validar/Obtener Rol Default de la Empresa
        Role role = roleRepository.findFirstByCompanyAndIsDefault(company, 1);
        if (role == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Configuración incompleta",
                    "La empresa no tiene un rol por defecto configurado");
        }

        // 3. Asignar Rol y Empresa al DTO
        UserCompanyRoleDTO ucrDTO = new UserCompanyRoleDTO();
        ucrDTO.setCompanyUuid(company.getUuid());

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setUuid(role.getUuid());
        roleDTO.setName(role.getName());

        ucrDTO.setRoles(List.of(roleDTO));
        userDTO.setUserCompanyRoles(List.of(ucrDTO));

        // 4. Encriptar contraseña
        if (userDTO.getPassword() != null) {
            userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // 5. Bypass saveOrCreate para evitar hookSaveAuth y manejar estado pendiente
        User user = (User) this.mapper.dtoToModel(userDTO, new User());
        user.setStatus(-2);
        generateAndSetVerificationCode(user);

        // 6. Hook pre save
        user = hookPublicRegisterPreSave(user, company);

        user = (User) this.repository.save(user);

        // 7. Enviar email de verificación
        sendVerificationEmail(user, null);

        return this.mapper.modelToDto(user);
    }

    public void verifyCode(String uuid, String code) {
        User user = (User) this.repository.findFirstByUuid(uuid);
        if (user == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "User not found: " + uuid);
        }

        if (user.getStatus() != -2) {
            // Ya verificado o en otro estado
            if (user.getStatus() == 0)
                return; // Ya activo
            throw new CustomException(HttpStatus.BAD_REQUEST, "El usuario no está pendiente de verificación",
                    "Status invalid: " + user.getStatus());
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Código inválido", "Verification code mismatch");
        }

        if (user.getVerificationCodeExpiry().isBefore(Instant.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código ha expirado", "Verification code expired");
        }

        // Codigo Valido
        user.setStatus(0); // ACTIVO
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        user.setVerificationCodeLastSent(null);

        this.repository.save(user);
    }

    public void resendCode(String uuid, String email, Company company) {
        User user = (User) this.repository.findFirstByUuid(uuid);
        if (user == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "User not found: " + uuid);
        }

        // Verificar Cooldown (3 min)
        if (user.getVerificationCodeLastSent() != null) {
            Instant cooldownEnd = user.getVerificationCodeLastSent().plus(3, ChronoUnit.MINUTES);
            if (Instant.now().isBefore(cooldownEnd)) {
                throw new CustomException(HttpStatus.TOO_MANY_REQUESTS,
                        "Por favor espere unos minutos antes de reenviar el código", "Cooldown active");
            }
        }

        // Actualizar email si es diferente
        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
            user.setEmail(email);
        }

        generateAndSetVerificationCode(user);
        this.repository.save(user);

        sendVerificationEmail(user, company);
    }

    protected void sendVerificationEmail(User user, Company company) {
        try {
            MailNotification mail = hookGetMailNotification(PURPOSE_REGISTRATION, user, user.getVerificationCode(),
                    company);
            javaMailSender.sendEmail(user.getEmail(), mail.getSubject(), mail.getBody());
        } catch (Exception e) {
            throw new CustomException("Error al enviar el email de verificación", e.getMessage());
        }
    }

    protected void generateAndSetVerificationCode(User user) {
        String code = generateRandomAlphanumeric(6);
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(Instant.now().plus(10, ChronoUnit.MINUTES));
        user.setVerificationCodeLastSent(Instant.now());
    }

    protected String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Hook para validar si un usuario puede asignar un rol específico.
     * Por defecto, el núcleo (Core) prohíbe asignar roles que pertenezcan a la
     * empresa de sistema (UUID 1).
     */
    protected boolean hookCanAssignRole(Role role, User user, String companyUuid) {
        boolean isSysRole = role.getCompany() != null
                && (Long.valueOf(1).equals(role.getCompany().getId())
                        || "1".equals(role.getCompany().getUuid()));

        return !isSysRole;
    }

    /**
     * Hook utilizado previo al guardado final de un usuario en un registro público.
     * Útil para implementar lógica de negocio secundaria, como la vinculación de
     * entidades creadas.
     */
    protected User hookPublicRegisterPreSave(User user, Company company) {
        return user;
    }

    /**
     * Hook utilizado previo a la creación de la empresa en un registro público.
     * Útil para validar datos de usuario (email, username) o de la empresa (CUIT).
     */
    protected void hookPublicRegisterPreCreateCompany(DTO userDTO, CompanyDTO companyDTO) {
    }

    public UserAccessToken createAccessToken(String name) {
        return authService.createAccessToken(name);
    }

    public UserAccessToken deleteAccessToken(String uuid) {
        return authService.deleteAccessToken(uuid);
    }

    public List<UserAccessToken> getMyTokens() {
        return authService.getMyTokens();
    }

    public UserAccessTokenDTO createAccessTokenDto(String name) {
        return userAccessTokenMapper.modelToDto(createAccessToken(name));
    }

    public UserAccessTokenDTO deleteAccessTokenDto(String uuid) {
        return userAccessTokenMapper.modelToDto(deleteAccessToken(uuid));
    }

    public List<UserAccessTokenDTO> getMyTokenDtos() {
        return userAccessTokenMapper.modelsToDtos(getMyTokens());
    }

    public void forgotPassword(String email, String path) {
        if (email == null || email.trim().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos incompletos", "Email is required");
        }

        User user = userRepository.findFirstByEmailAndStatus(email, 0);
        if (user == null) {
            Logger.getLogger(BaseUserService.class.getName()).info("Usuario no encontrado: " + email);
            return;
        }

        Company company = (path != null && !path.trim().isEmpty())
                ? companyRepository.findFirstByPath(path.trim())
                : null;

        SecurityCode prevCode = securityCodeRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, 0);
        if (prevCode != null) {
            Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (prevCode.getCreatedAt().isAfter(fiveMinutesAgo)) {
                throw new CustomException(HttpStatus.TOO_MANY_REQUESTS,
                        "Ya te enviamos un mail. Por favor, espera 5 minutos para solicitar otro.",
                        "Cooldown active: email request within 5 minutes");
            }
            prevCode.setStatus(-1);
            prevCode.setDateDeleted(Timestamp.from(Instant.now()));
            securityCodeRepository.save(prevCode);
        }

        String code = generateRandomAlphanumeric(32);

        SecurityCode securityCode = new SecurityCode();
        securityCode.setCode(code);
        securityCode.setCreatedAt(Instant.now());
        securityCode.setPurpose("FORGOT_PASSWORD");
        securityCode.setUsed(false);
        securityCode.setUser(user);
        securityCodeRepository.save(securityCode);

        sendForgotPasswordEmail(user, code, company);
    }

    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findFirstByEmailAndStatus(changePasswordDTO.getEmail(), 0);
        if (user == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "No existe el usuario", "No existe el usuario");
        }

        SecurityCode code = securityCodeRepository.findFirstByCodeAndUser(changePasswordDTO.getCode(), user);
        if (code == null) {
            throw new CustomException("Código inválido");
        }

        if (code.getUsed() != null && code.getUsed()) {
            throw new CustomException("El código ya fue utilizado");
        }

        Instant minutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        if (code.getCreatedAt().isBefore(minutesAgo)) {
            code.setStatus(-1);
            code.setDateDeleted(Timestamp.from(Instant.now()));
            securityCodeRepository.save(code);
            throw new CustomException("El código ya venció");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getPassword()));
        this.repository.save(user);

        code.setUsed(true);
        code.setStatus(-1);
        code.setDateDeleted(Timestamp.from(Instant.now()));
        securityCodeRepository.save(code);
    }

    protected void sendForgotPasswordEmail(User user, String code, Company company) {
        try {
            MailNotification mail = hookGetMailNotification(PURPOSE_FORGOT_PASSWORD, user, code, company);
            javaMailSender.sendEmail(user.getEmail(), mail.getSubject(), mail.getBody());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al enviar el email de recuperación de contraseña",
                    "BaseUserService | sendForgotPasswordEmail | error: " + e.getMessage());
        }
    }

    protected MailNotification hookGetMailNotification(String purpose, User user, String code, Company company) {
        throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "No Implementado",
                "La aplicación debe implementar el hookGetMailNotification para el propósito: " + purpose);
    }
}
