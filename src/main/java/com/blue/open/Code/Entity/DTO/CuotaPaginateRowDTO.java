package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CuotaPaginateRowDTO {

    private String cuotaUuid;
    private String concepto;
    private CuotaTipoEnum tipo;
    private BigDecimal monto;
    private Integer descuento;
    private BigDecimal montoFinal;
    private BigDecimal recargo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate vencimiento;
    private CuotaEstadoEnum estado;
    private Integer diasMora;

    private String personaMemberUuid;
    private String personaNombre;
    private String personaApellido;
    private MemberEstadoEnum personaEstado;

    private String titularMemberUuid;
    private String titularNombre;
    private String titularApellido;
    private String titularEmail;
    private String titularTelefono;
}
