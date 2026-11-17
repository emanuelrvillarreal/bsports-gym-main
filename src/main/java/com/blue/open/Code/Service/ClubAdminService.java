package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.UserMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Crea el usuario administrador de un club: el User y su UserCompanyRole con el rol ADMIN.
 * <p>
 * Lo usan el alta por sysadmin (SysadminClubService) y el alta parcial desde el webhook
 * ALTA_CLIENTE del CRM (CompanyService.registrarAltaCliente).
 * <p>
 * Espera que los roles del club ya esten provisionados (ClubRoleProvisioningService.provisionRoles).
 */
@Service
public class ClubAdminService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubRoleProvisioningService clubRoleProvisioningService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Crea el admin del club dado de alta por ALTA_CLIENTE, en una transaccion propia: corre
     * despues de confirmada la Company, asi que si falla no la revierte.
     * <p>
     * Espera: el id del club, el correo de facturacion (email y username del admin), la razon
     * social (nombre del admin) y el CUIT, que se usa como contraseña inicial.
     * Responde: nada. Lanza si el club no existe, si no tiene rol ADMIN o si el email ya es de
     * otro usuario; quien lo invoca decide que hacer con la falla.
     */
    @Transactional
    public void crearAdminDeAltaCliente(Long companyId, String email, String nombre, String cuit) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                        "ClubAdminService | crearAdminDeAltaCliente | companyId: " + companyId));

        crearAdmin(company, email, nombre, null, cuit);

        company.marcarAdminCreado();
        companyRepository.save(company);
    }

    /**
     * Crea el usuario administrador del club con el rol ADMIN de esa company.
     * <p>
     * Espera: el club, el email (que tambien es el username), nombre y apellido (pueden ser null)
     * y la contraseña en texto plano, que se guarda encriptada.
     * Responde: el usuario creado. Lanza si el email ya pertenece a un usuario o si el club no
     * tiene rol ADMIN provisionado.
     */
    public User crearAdmin(Company company, String email, String nombre, String apellido, String password) {
        String emailLimpio = email.trim();
        validarEmailDisponible(emailLimpio);

        Role adminRole = clubRoleProvisioningService.getAdminRole(company);
        if (adminRole == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Configuración incompleta",
                    "Rol ADMIN no encontrado para el club recién creado.");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setNombre(nombre);
        userDTO.setApellido(apellido);
        userDTO.setEmail(emailLimpio);
        userDTO.setUsername(emailLimpio);
        userDTO.setPassword(passwordEncoder.encode(password));

        User user = userMapper.dtoToModel(userDTO, new User());
        user.setStatus(0);

        user = userRepository.save(user);

        UserCompanyRole userCompanyRole = new UserCompanyRole();
        userCompanyRole.setUser(user);
        userCompanyRole.setCompany(company);
        userCompanyRole.setRoles(new ArrayList<>(List.of(adminRole)));
        userCompanyRole = userCompanyRoleRepository.save(userCompanyRole);

        if (user.getUserCompanyRoles() == null) {
            user.setUserCompanyRoles(new ArrayList<>());
        }
        user.getUserCompanyRoles().add(userCompanyRole);
        return userRepository.save(user);
    }

    private void validarEmailDisponible(String email) {
        if (userRepository.findFirstByUsernameIgnoringCase(email) == null) {
            return;
        }
        throw new CustomException(HttpStatus.BAD_REQUEST, "Email ya registrado",
                "ClubAdminService | crearAdmin | ya existe un usuario con el email: " + email);
    }
}
