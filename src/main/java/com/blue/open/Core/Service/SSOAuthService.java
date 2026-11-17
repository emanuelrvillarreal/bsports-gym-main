package com.blue.open.Core.Service;

import com.blue.open.Core.Entity.DTO.UsuarioSSO;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SSOAuthService {

    @Value("${sso.url:#{null}}")
    private String ssoUrl;

    @Value("${sso.clientId:#{null}}")
    private String ssoclientId;

    private UsuarioSSO usuarioSSO;

    private void validateConfiguration() {
        if (ssoUrl == null || ssoclientId == null) {
            throw new CustomException("Faltan configuraciones necesarias para el servicio SSO", "No existe sso.url o sso.clientId en el application.properties");
        }
    }

    // Obtiene el usuario actualmente conectado usando desde el servicio de SSO
    public UsuarioSSO getUserSSO() {
        try {
            validateConfiguration();

            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Contexto de petición no disponible",
                        "SSOAuthService | getUserSSO | RequestContextHolder sin atributos");
            }

            HttpServletRequest request = requestAttributes.getRequest();
            String bearer = request.getHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Token inválido",
                        "SSOAuthService | getUserSSO | No se encontró el token Bearer en la cabecera");
            }
            bearer = bearer.replace("Bearer ", "");
            this.usuarioSSO = this.obtenerUsuariosAplicacion(bearer).getBody();
            if (this.usuarioSSO == null) {
                throw new CustomException(HttpStatus.UNAUTHORIZED,
                        "No se pudo autenticar con el sistema de usuarios",
                        "SSOAuthService | getUserSSO | SSO no devolvió usuario para el token recibido");
            }
            return this.usuarioSSO;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el usuario", "SSOAuthService | getUserSSO | error: " + e.getMessage());
        }
    }

    // Obitiene un usuario según un token. No necesesita ser el actualmente conectado
    public ResponseEntity<UsuarioSSO> obtenerUsuariosAplicacion(String accessToken) {
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
                        UsuarioSSO.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return response;
                } else {
                    throw new CustomException("Respuesta fallida del sistema de usuario", "SSOAuthService | obtenerUsuariosAplicacion | code: " + response.getStatusCode() + " | body: " + response.getBody());
                }
            } catch (CustomException e) {
                throw e;
            } catch (HttpClientErrorException ex) {
                System.out.println("Error en la llamada al SSO " + ex.getStatusCode() + ex.getResponseBodyAsString());
                throw new CustomException("Error en la llamada al sistema de usuario", "SSOAuthService | obtenerUsuariosAplicacion | code: " + ex.getStatusCode() + " | body: " + ex.getResponseBodyAsString() + " message: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Error al obtener la informacion del SSO " + ex.getMessage());
                throw new CustomException("Error al obtener la informacion del sistema de usuario", "SSOAuthService | obtenerUsuariosAplicacion | error: " + ex.getMessage());
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error en la llamada para obtener el usuario", "SSOAuthService | obtenerUsuariosAplicacion | error: " + e.getMessage());
        }
    }

    // Comprueba si el usuario actualmente conectado tiene alguno de los roles pasados como parametro
    public boolean validBoolean(List<String> validRoles) {
        try {
            validateConfiguration();
            List<String> userRoles = this.getUserSSO().getRoles();
            for (String validRol : validRoles) {
                for (String userRol : userRoles) {
                    if (Objects.equals(validRol, userRol)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error en la obtencion de permisos del usuario", "SSOAuthService | validBoolean | error: " + e.getMessage());
        }
    }

    // Comprueba si el usuario actualmente conectado tiene alguno de los roles pasados como parametro. Si es false corta la ejecucion de la llamada
    public void valid(List<String> permisosRequeridos) {
        try {
            validateConfiguration();
            if (!this.validBoolean(permisosRequeridos)) {
                throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso", "Falta de permisos para acceder a la ruta");
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error en la validacion de permisos del usuario", "SSOAuthService | valid | error: " + e.getMessage());
        }
    }
}
