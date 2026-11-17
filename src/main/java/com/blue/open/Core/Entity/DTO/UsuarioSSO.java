package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UsuarioSSO {
    private String uuid;
    private String username;
    private String email;
    private String nombre;
    private String telefono;
    private Boolean cuentaVerificada;
    private Boolean cuentaBloqueada;
    private List<String> roles;
}
