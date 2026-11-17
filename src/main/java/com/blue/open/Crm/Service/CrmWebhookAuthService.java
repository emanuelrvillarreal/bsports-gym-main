package com.blue.open.Crm.Service;

import com.blue.open.Core.Exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Valida la autenticacion de los webhooks entrantes del CRM (blue-crm).
 * <p>
 * El CRM envia sus webhooks con el header Authorization: Bearer seguido del token del sistema
 * externo, que es el mismo valor configurado en crm.api.token. Este servicio comprueba que el
 * token recibido coincida con el configurado.
 * <p>
 * Depende de: la property crm.api.token. Es la misma que usa CrmRestClientConfig para las
 * llamadas salientes, pero se lee aca por separado para no exponer el token con un getter.
 * <p>
 * Nota de seguridad: el endpoint de webhooks queda publico porque SecurityConfig permite todas
 * las rutas y la autenticacion se resuelve en cada controller. Sin esta validacion cualquiera
 * podria invocarlo.
 */
@Service
@Slf4j
public class CrmWebhookAuthService {

    private static final String PREFIJO_BEARER = "bearer ";

    private final String apiToken;

    public CrmWebhookAuthService(@Value("${crm.api.token:#{null}}") String apiToken) {
        this.apiToken = apiToken;
    }

    /**
     * Valida el header Authorization de un webhook entrante del CRM.
     * <p>
     * Espera: el contenido del header Authorization, con formato "Bearer {token}".
     * Responde: nada si el token es valido.
     * Lanza CustomException 401 si el token falta, tiene otro formato o no coincide con el
     * configurado. Tambien lanza 401 si crm.api.token no esta configurado, para no dejar el
     * endpoint abierto por una configuracion incompleta.
     */
    public void validarAutorizacion(String authorization) {
        if (this.apiToken == null || this.apiToken.isBlank()) {
            log.error("CrmWebhookAuthService | validarAutorizacion | falta crm.api.token, "
                    + "se rechazan los webhooks del CRM");
            throw new CustomException(HttpStatus.UNAUTHORIZED, "No autorizado",
                    "CrmWebhookAuthService | validarAutorizacion | falta la property crm.api.token");
        }

        if (authorization == null || authorization.isBlank()) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "No autorizado",
                    "CrmWebhookAuthService | validarAutorizacion | falta el header Authorization");
        }

        String header = authorization.trim();
        if (header.length() <= PREFIJO_BEARER.length()
                || !header.substring(0, PREFIJO_BEARER.length()).toLowerCase().equals(PREFIJO_BEARER)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "No autorizado",
                    "CrmWebhookAuthService | validarAutorizacion | el header Authorization no es Bearer");
        }

        String tokenRecibido = header.substring(PREFIJO_BEARER.length()).trim();
        // Comparacion en tiempo constante para no filtrar el token por diferencia de tiempos
        boolean coincide = MessageDigest.isEqual(
                tokenRecibido.getBytes(StandardCharsets.UTF_8),
                this.apiToken.trim().getBytes(StandardCharsets.UTF_8));

        if (!coincide) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "No autorizado",
                    "CrmWebhookAuthService | validarAutorizacion | el token recibido no coincide con crm.api.token");
        }
    }
}
