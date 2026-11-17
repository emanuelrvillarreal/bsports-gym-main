package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.SexoEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {

    private String userUuid;
    private String nombre;
    private String apellido;
    private String dni;
    private SexoEnum sexo;
    private String telefono;
    private String email;
}
