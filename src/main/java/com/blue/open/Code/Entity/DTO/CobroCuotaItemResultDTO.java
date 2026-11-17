package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CobroCuotaItemResultDTO {

    private String cuotaUuid;
    private boolean exitoso;
    private String movimientoUuid;
    private String mensajeError;
}
