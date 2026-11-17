package com.blue.open.Code.Entity.DTO;

import com.blue.open.Crm.Entity.DTO.Request.CrmDescuentoRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Cambio del plan de un club que ya es cliente del CRM. */
@Getter
@Setter
public class SysadminCambioPlanRequestDTO {

    @NotBlank(message = "El código de plan es requerido")
    private String codigoPlan;

    // Opcional. Si viaja null, la suscripcion al plan nuevo se crea sin descuento
    private CrmDescuentoRequestDTO descuento;
}
