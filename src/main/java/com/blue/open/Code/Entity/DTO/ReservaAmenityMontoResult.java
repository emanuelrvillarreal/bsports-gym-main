package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReservaAmenityMontoResult {

    private BigDecimal montoTotal;
    private BigDecimal montoSena;
    private BigDecimal saldoPendiente;
}
