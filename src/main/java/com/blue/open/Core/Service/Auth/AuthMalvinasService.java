package com.blue.open.Core.Service.Auth;

// Ultimo cambio:
// Autor: Taiel Martinez
// Fecha: 17/11/2025
// Descripcion: Primera vez que se implementa el auth de malvinas

import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Mapper.RoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

import static com.blue.open.Core.Service.Utils.isValidString;

@Slf4j
@Component
public class AuthMalvinasService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    BaseCompanyRepository companyRepository;
    @Autowired
    JwtDecoder jwtDecoder;
    @Autowired
    RoleMapper roleMapper;

    @Value("${auth.malvinas.url:#{null}}")
    private String malvinasUrl;

    public User getUser() {
        try {
            if (malvinasUrl == null) {
                throw new CustomException(
                        "Faltan configuraciones necesarias para el servicio autenticacion de malvinas",
                        "No existe auth.malvinas.url en el application.properties");
            }

            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String bearer = request.getHeader("Authorization");
                if (bearer == null || !bearer.startsWith("Bearer ")) {
                    return null;
                }
                bearer = bearer.replace("Bearer ", "");
                if (!isValidString(bearer)) {
                    return null;
                }
                if (!isJwtFormat(bearer)) {
                    return null;
                }

                Jwt jwt = jwtDecoder.decode(bearer);
                    if (!isValidString(jwt.getClaimAsString("urn:malvinas:legajo"))) {
                        log.error(
                                "AuthMalvinasService | getUser | Se pudo obtener al usuario, pero el mismo no tiene legajo");
                        throw new CustomException("Error al obtener el legajo del usuario",
                                "AuthMalvinasService | getUser | Se pudo obtener al usuario, pero el mismo no tiene legajo");
                    }
                    User user = userRepository
                            .findFirstByMalvinasLegajoAndStatus(jwt.getClaimAsString("urn:malvinas:legajo"), 0);
                    if (user == null) {
                        user = new User();
                        user.setMalvinasLegajo(jwt.getClaimAsString("urn:malvinas:legajo"));
                    }

                    if (!isValidString(user.getUsername())) {
                        user.setUsername(jwt.getClaimAsString("urn:malvinas:legajo"));
                    }

                    if (isValidString(jwt.getClaimAsString("name"))) {
                        user.setName(jwt.getClaimAsString("name"));
                    }

                    if (isValidString(jwt.getClaimAsString("email"))) {
                        user.setEmail(jwt.getClaimAsString("email"));
                    }

                    if (isValidString(jwt.getClaimAsString("phone_number"))) {
                        user.setPhone(jwt.getClaimAsString("phone_number"));
                    }

                    if (isValidString(jwt.getClaimAsString("urn:malvinas:dep:name"))) {
                        user.setDepartmentName(jwt.getClaimAsString("urn:malvinas:dep:name"));
                    }

                    if (isValidString(jwt.getClaimAsString("urn:malvinas:grp:name"))) {
                        user.setGroupName(jwt.getClaimAsString("urn:malvinas:grp:name"));
                    }

                    user = userRepository.save(user);

                    Company company = companyRepository.findFirstByNameAndStatus("malvinas", 0);
                    if (company != null) {
                        UserCompanyRole userCompanyRole = null;
                        if (user.getUserCompanyRoles() != null) {
                            for (UserCompanyRole ucr : user.getUserCompanyRoles()) {
                                if (ucr.getCompany() != null && company.getUuid().equals(ucr.getCompany().getUuid())) {
                                    userCompanyRole = ucr;
                                    break;
                                }
                            }
                        } else {
                            user.setUserCompanyRoles(new ArrayList<>());
                        }

                        if (userCompanyRole == null) {
                            userCompanyRole = new UserCompanyRole();
                            userCompanyRole.setUser(user);
                            userCompanyRole.setCompany(company);
                            user.getUserCompanyRoles().add(userCompanyRole);
                        }

                        if (jwt.getClaimAsStringList("role") != null) {
                            List<Role> roles = getModelsByListStrings(jwt.getClaimAsStringList("role"));
                            userCompanyRole.setRoles(roles);
                        }

                        user = userRepository.save(user);
                    }

                return user;
            }
            return null;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de autenticación Malvinas inválido",
                    "AuthMalvinasService | getUser | error: " + e.getMessage());
        }
    }

    private boolean isJwtFormat(String token) {
        return token.split("\\.", -1).length == 3;
    }

    private List<Role> getModelsByListStrings(List<String> roles) {
        List<Role> rolesList = new ArrayList<>();
        for (String rol : roles) {
            rolesList.add(roleMapper.dtoToModel(new RoleDTO(rol, 0, null), null));
        }
        return rolesList;
    }
}
