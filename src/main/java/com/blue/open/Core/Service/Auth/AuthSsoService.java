package com.blue.open.Core.Service.Auth;

// Ultimo cambio:
// Autor: Taiel Martinez
// Fecha: 17/11/2025
// Descripcion: Se simplifica el archivo para integrarlo en la validacion por authService unificado

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Mapper.RoleMapper;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.DTO.UsuarioSSO;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blue.open.Core.Service.Utils.isValidString;

@Slf4j
@Component
public class AuthSsoService {

    @Autowired
    UserRepository repository;
    @Autowired
    BaseCompanyRepository companyRepository;
    @Autowired
    RoleMapper roleMapper;

    @Value("${auth.sso.url:#{null}}")
    private String ssoUrl;

    @Value("${auth.sso.clientId:#{null}}")
    private String ssoclientId;

    private void validateConfiguration() {
        if (ssoUrl == null || ssoclientId == null) {
            throw new CustomException("Faltan configuraciones necesarias para el servicio SSO",
                    "No existe auth.sso.url o auth.sso.clientId en el application.properties");
        }
    }

    // Obtiene el usuario actualmente conectado usando desde el servicio de SSO
    public User getUser() {
        try {
            validateConfiguration();

            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String bearer = request.getHeader("Authorization");
                if (bearer == null || !bearer.startsWith("Bearer ")) {
                    return null;
                }
                bearer = bearer.replace("Bearer ", "");
                UsuarioSSO usuarioSSO = this.obtenerUsuariosAplicacion(bearer).getBody();
                if (usuarioSSO == null) {
                    throw new CustomException(HttpStatus.UNAUTHORIZED,
                            "No se pudo autenticar con el sistema de usuarios",
                            "AuthSsoService | getUser | SSO no devolvió usuario para el token recibido");
                }
                return mapperUserBySSO(usuarioSSO);
            }
            return null;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el usuario",
                    "SSOAuthService | getUserSSO | error: " + e.getMessage());
        }
    }

    // Obitiene un usuario según un token. No necesesita ser el actualmente
    // conectado
    private ResponseEntity<UsuarioSSO> obtenerUsuariosAplicacion(String accessToken) {
        try {
            validateConfiguration();

            String url = this.ssoUrl + "/usuarios/aplicacion";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("clientId", this.ssoclientId);
            requestBody.put("accessToken", accessToken);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<UsuarioSSO> response;
            try {
                response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        UsuarioSSO.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return response;
                } else {
                    throw new CustomException("Respuesta fallida del sistema de usuario",
                            "SSOAuthService | obtenerUsuariosAplicacion | code: " + response.getStatusCode()
                                    + " | body: " + response.getBody());
                }
            } catch (CustomException e) {
                throw e;
            } catch (HttpClientErrorException ex) {
                System.out.println("Error en la llamada al SSO " + ex.getStatusCode() + ex.getResponseBodyAsString());
                throw new CustomException("Error en la llamada al sistema de usuario",
                        "SSOAuthService | obtenerUsuariosAplicacion | code: " + ex.getStatusCode() + " | body: "
                                + ex.getResponseBodyAsString() + " message: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Error al obtener la informacion del SSO " + ex.getMessage());
                throw new CustomException("Error al obtener la informacion del sistema de usuario",
                        "SSOAuthService | obtenerUsuariosAplicacion | error: " + ex.getMessage());
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error en la llamada para obtener el usuario",
                    "SSOAuthService | obtenerUsuariosAplicacion | error: " + e.getMessage());
        }
    }

    public User mapperUserBySSO(UsuarioSSO usuarioSSO) {
        User user = repository.findFirstBySsoUuidAndStatus(usuarioSSO.getUuid(), 0);
        if (user == null) {
            user = new User();
            if (isValidString(usuarioSSO.getUuid()))
                user.setSsoUuid(usuarioSSO.getUuid());
        }

        if (isValidString(usuarioSSO.getUsername()))
            user.setUsername(usuarioSSO.getUsername());
        if (isValidString(usuarioSSO.getEmail()))
            user.setEmail(usuarioSSO.getEmail());
        if (isValidString(usuarioSSO.getTelefono()))
            user.setPhone(usuarioSSO.getTelefono());

        user = repository.save(user);

        Company company = companyRepository.findFirstByNameAndStatus("sso", 0);
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

            if (usuarioSSO.getRoles() != null) {
                List<Role> roles = getModelsByListStrings(usuarioSSO.getRoles());
                userCompanyRole.setRoles(roles);
            }

            user = repository.save(user);
        }

        return user;
    }

    private List<Role> getModelsByListStrings(List<String> roles) {
        List<Role> rolesList = new ArrayList<>();
        for (String rol : roles) {
            rolesList.add(roleMapper.dtoToModel(new RoleDTO(rol, 0, null), null));
        }
        return rolesList;
    }
}
