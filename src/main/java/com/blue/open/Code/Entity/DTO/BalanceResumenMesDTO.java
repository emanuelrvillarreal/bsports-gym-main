package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
public class BalanceResumenMesDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/yyyy")
    private YearMonth periodo;
    private int mes;
    private int anio;
    private BigDecimal ingresosTotales;
    private BigDecimal egresosTotales;
    private BigDecimal porCobrar;
    private BigDecimal ingresosNetos;
    private BigDecimal ingresosEstimados;
}
