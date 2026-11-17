package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LiquidacionCuotaDetalle {

    private MedioPagoEnum medioPago;
    private LocalDate fecha;
    private String observaciones;
}
