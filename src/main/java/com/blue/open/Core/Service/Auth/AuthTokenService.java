package com.blue.open.Core.Service.Auth;

import com.blue.open.Code.Entity.Db.User;

// Ultimo cambio:
// Autor: Taiel Martinez
// Fecha: 17/11/2025
// Descripcion: Se crea la validacion por token

import com.blue.open.Core.Entity.Db.UserAccessToken;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserAccessTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.util.List;

import static com.blue.open.Core.Service.Utils.isValidString;

@Component
public class AuthTokenService {
    @Autowired
    UserAccessTokenRepository repository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    public User getUser() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (requestAttributes == null) {
                return null;
            }

            HttpServletRequest request = requestAttributes.getRequest();
            String bearer = request.getHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                return null;
            }

            bearer = bearer.replace("Bearer ", "");
            if (!isValidString(bearer)) {
                return null;
            }

            if (isJwtFormat(bearer)) {
                return null;
            }

            UserAccessToken model = repository.findFirstByCode(bearer);
            if (model == null || model.getUser() == null) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso personal inválido",
                        "AuthTokenService | getUser | Token no encontrado o sin usuario asociado");
            }

            return (User) model.getUser();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el usuario",
                    "AuthTokenService | getUser | error: " + e.getMessage());
        }
    }

    private boolean isJwtFormat(String token) {
        return token.split("\\.", -1).length == 3;
    }

    /**
     * Crea un nuevo token de acceso de 128 caracteres para un usuario.
     * Los tokens son permanentes hasta que se desactiven (active=0) o se eliminen
     * (status=-1).
     *
     * @param user El usuario al que se asocia el token
     * @param name Nombre o descripción del token
     * @return UserAccessToken persistido
     */
    public UserAccessToken createAccessToken(User user, String name) {
        try {
            // Validar unicidad del nombre para el usuario actual (status=0 es activo)
            UserAccessToken existing = repository.findFirstByNameAndUserAndStatus(name, user, 0);
            if (existing != null) {
                throw new CustomException("Ya existe un token con el nombre: " + name);
            }

            UserAccessToken token = new UserAccessToken();
            token.setUser(user);
            token.setName(name);
            token.setCode(generateRandomToken(128));
            token.setActive(1);
            return repository.save(token);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al crear el token de acceso",
                    "AuthTokenService | createAccessToken | error: " + e.getMessage());
        }
    }

    private String generateRandomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Realiza un borrado lógico de un token de acceso.
     * Valida que el token pertenezca al usuario indicado.
     *
     * @param user Usuario propietario esperado
     * @param uuid UUID del token a eliminar
     * @return UserAccessToken eliminado
     */
    public UserAccessToken deleteAccessToken(User user, String uuid) {
        try {
            UserAccessToken token = repository.findFirstByUuidWithStatusIsActive(uuid);
            if (token == null) {
                throw new CustomException("No existe el token solicitado o ya fue eliminado");
            }

            if (token.getUser() == null || !token.getUser().getUuid().equals(user.getUuid())) {
                throw new CustomException("No tienes permisos para eliminar este token");
            }

            token.setStatus(-1);
            token.setActive(0);
            token.setDateDeleted(java.sql.Timestamp.from(java.time.Instant.now()));

            return repository.save(token);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al eliminar el token de acceso",
                    "AuthTokenService | deleteAccessToken | error: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los tokens activos de un usuario.
     *
     * @param user Usuario propietario
     * @return Lista de UserAccessToken
     */
    public List<UserAccessToken> getTokensByUser(User user) {
        return repository.findAllByUserAndStatus(user, 0);
    }
}
