package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EventoAccesoCobroResultDTO {

    private String eventoUuid;
    private EventoAccesoHistorialTipoEnum tipo;
    private EventoAccesoScanEstadoEnum resultado;
    private Integer cantidadEntradas;
    private BigDecimal monto;
    private String concepto;
    private String movimientoUuid;
    private String qr;
    private String memberUuid;
    private String nombre;
    private String apellido;
    private String numeroSocio;
}
