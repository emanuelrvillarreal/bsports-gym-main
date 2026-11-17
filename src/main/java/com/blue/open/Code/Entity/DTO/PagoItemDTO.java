package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PagoItemDTO {

    private String uuid;
    private String titulo;
    private String detalle;
    private BigDecimal monto;
    private String cuotaUuid;
}
