package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CuotaAnularCommand {

    @NotBlank(message = "Motivo de anulación requerido")
    @Size(max = 512, message = "Motivo demasiado largo")
    private String motivo;

    /** Solo cuotas de actividad: además de anular, da de baja al socio de esa actividad (cuota cargada por error). */
    private boolean darDeBajaActividad;
}
