package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DescuentoDTO {

    private String descuentoUuid;
    private String titulo;
    private DescuentoTipoValorEnum tipoValor;
    private Integer porcentaje;
    private BigDecimal montoFijo;
}
