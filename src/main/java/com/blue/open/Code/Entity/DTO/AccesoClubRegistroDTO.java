package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.AccesoClubTipoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AccesoClubRegistroDTO {

    private String accesoUuid;
    private String userUuid;
    private String nombre;
    private String apellido;
    private String dni;
    private AccesoClubTipoEnum tipo;
    private MemberEstadoEnum memberEstado;
    private String advertencia;
    private boolean presente;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fecha;
}
