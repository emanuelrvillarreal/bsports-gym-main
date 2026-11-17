package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CuotaSocialRangoDTO {

    private String cuotaSocialRangoUuid;
    private String nombre;
    private String descripcion;
    private Integer edadHasta;
    private BigDecimal importe;
    private Boolean esDefault;
}
