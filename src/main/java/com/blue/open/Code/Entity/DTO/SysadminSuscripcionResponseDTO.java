package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Estado de la suscripcion del club al plan del CRM, tal como quedo despues de la operacion. */
@Getter
@Setter
public class SysadminSuscripcionResponseDTO {

    private String companyUuid;
    private String codigoPlan;
    private String nombrePlan;
    private String uuidSuscripcionCrm;
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime fechaProximaFacturacion;
}
