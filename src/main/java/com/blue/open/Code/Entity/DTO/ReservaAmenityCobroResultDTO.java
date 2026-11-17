package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReservaAmenityCobroResultDTO {

    private String reservaUuid;
    private ReservaAmenityCobroTipoEnum tipoCobro;
    private BigDecimal monto;
    private String concepto;
    private String movimientoUuid;
    private String qr;
}
