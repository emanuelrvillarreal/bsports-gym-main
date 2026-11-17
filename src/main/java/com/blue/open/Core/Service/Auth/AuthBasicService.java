package com.blue.open.Core.Service.Auth;

import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class AuthBasicService {
    @Autowired
    UserRepository repository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String[] getAuth() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Basic ")) {
                    String base64Credentials = authHeader.substring("Basic ".length());
                    byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
                    String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

                    // Divide en username y password
                    return decoded.split(":", 2);
                }
            }
            return new String[0];
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener y validar el user", "getAuth: " + e.getMessage());
        }
    }

    public User getUser() {
        try {
            if (!hasBasicAuthorizationHeader()) {
                return null;
            }

            String[] auth = this.getAuth();
            if (auth.length < 2) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Credenciales Basic inválidas",
                        "AuthBasicService | getUser | Formato de credenciales incorrecto");
            }

            User user = repository.findFirstByUsernameAndStatus(auth[0], 0);
            if (user == null) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos",
                        "AuthBasicService | getUser | Usuario no encontrado: " + auth[0]);
            }

            if (!passwordEncoder.matches(auth[1], user.getPassword())) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos",
                        "AuthBasicService | getUser | Contraseña incorrecta para usuario: " + auth[0]);
            }

            return user;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el user", "getUser: " + e.getMessage());
        }
    }

    private boolean hasBasicAuthorizationHeader() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        String authHeader = requestAttributes.getRequest().getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Basic ");
    }
}
