package com.blue.open.Code.Entity.DTO;

import com.blue.open.Crm.Entity.DTO.Request.CrmDescuentoRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta de la suscripcion de un club existente a un plan del CRM.
 */
@Getter
@Setter
public class SysadminSuscripcionRequestDTO {

    @NotBlank(message = "El código de plan es requerido")
    private String codigoPlan;

    // Opcional. Si viaja null, la suscripcion se crea sin descuento
    private CrmDescuentoRequestDTO descuento;
}
