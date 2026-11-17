package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DescuentoAsignadoDTO {

    private String asignacionUuid;
    private String descuentoUuid;
    private String titulo;
    private DescuentoTipoValorEnum tipoValor;
    private Integer porcentaje;
    private BigDecimal montoFijo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private boolean vigente;
}
