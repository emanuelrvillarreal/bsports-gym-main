package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ReservaAmenityTipoPagoInicialEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PortalReservaAmenityAltaCommand {

    @NotNull(message = "Fecha desde requerida")
    private LocalDateTime fechaDesde;

    @NotNull(message = "Fecha hasta requerida")
    private LocalDateTime fechaHasta;

    @NotNull(message = "Tipo de pago requerido")
    private ReservaAmenityTipoPagoInicialEnum tipoPago;
}
