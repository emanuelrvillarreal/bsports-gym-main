package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EventoAccesoScanResultDTO {

    private String memberUuid;
    private String nombre;
    private String apellido;
    private String numeroSocio;
    private EventoAccesoScanEstadoEnum estado;
    private String motivo;
    private BigDecimal montoAPagar;
}
