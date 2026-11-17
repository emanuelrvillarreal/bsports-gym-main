package com.blue.open.Code.Event;

import lombok.Getter;

/**
 * Publicado al generar un código de login sin contraseña.
 * {@link LoginSinPasswordSolicitadoEventListener} envía el mail con el enlace de forma asíncrona,
 * después del commit. Lleva solo datos planos: el código en claro no se persiste.
 */
@Getter
public class LoginSinPasswordSolicitadoEvent {

    private final String email;
    private final String nombreUsuario;
    private final String codigoPlano;

    // Path del club para la marca del mail. Puede ser null
    private final String path;

    public LoginSinPasswordSolicitadoEvent(String email, String nombreUsuario, String codigoPlano, String path) {
        this.email = email;
        this.nombreUsuario = nombreUsuario;
        this.codigoPlano = codigoPlano;
        this.path = path;
    }
}
