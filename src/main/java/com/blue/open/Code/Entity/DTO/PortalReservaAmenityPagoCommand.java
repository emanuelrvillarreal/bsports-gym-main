package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ReservaAmenityTipoPagoInicialEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortalReservaAmenityPagoCommand {

    private ReservaAmenityTipoPagoInicialEnum tipoPago;
}
