package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CuotaResumenDTO {

    private String cuotaUuid;
    private String concepto;
    private CuotaTipoEnum tipo;
    private BigDecimal monto;
    private Integer descuento;
    private BigDecimal montoFinal;
    /** Recargo por pago fuera de término ya incluido en montoFinal; 0 si no tiene. */
    private BigDecimal recargo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate vencimiento;
    private CuotaEstadoEnum estado;
    private Integer diasMora;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaGeneracion;
    private Long actividadId;
}
