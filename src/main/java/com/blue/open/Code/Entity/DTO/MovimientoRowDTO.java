package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoOrigenEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MovimientoRowDTO {

    private String movimientoUuid;
    private MovimientoTipoEnum tipo;
    private BigDecimal monto;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fecha;
    private String concepto;
    private MedioPagoEnum medioPago;
    private String observaciones;
    private MovimientoEstadoEnum estado;
    private MovimientoOrigenEnum origen;
    private String cuotaUuid;
    private String reservaUuid;
    private String personaMemberUuid;
    private String personaNombre;
    private String personaApellido;
    private String titularMemberUuid;
    private String registradoPorUuid;
    private String registradoPorNombre;
    private String registradoPorApellido;
}
