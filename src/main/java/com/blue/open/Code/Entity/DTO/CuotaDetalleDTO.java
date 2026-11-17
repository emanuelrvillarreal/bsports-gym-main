package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CuotaDetalleDTO extends CuotaResumenDTO {

    private String personaMemberUuid;
    private String personaNombre;
    private String personaApellido;
    private ParentescoEnum parentesco;
    private MemberEstadoEnum personaEstado;

    private String titularMemberUuid;
    private String titularNombre;
    private String titularApellido;

    private String anuladaPorUuid;
    private String anuladaPorNombre;
    private String anuladaPorApellido;
    private String anuladaMotivo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime anuladaAt;
}
