package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DescuentoEdicionCommand {

    @NotBlank(message = "Título requerido")
    private String titulo;

    @NotNull(message = "Tipo de valor requerido")
    private DescuentoTipoValorEnum tipoValor;

    private Integer porcentaje;

    private BigDecimal montoFijo;
}
