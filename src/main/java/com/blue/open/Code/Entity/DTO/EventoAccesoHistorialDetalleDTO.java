package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
public class EventoAccesoHistorialDetalleDTO {

    private String uuid;
    private EventoAccesoHistorialTipoEnum tipo;
    private String nombre;
    private EventoAccesoScanEstadoEnum resultado;
    private String motivo;
    private BigDecimal monto;
    private String memberUuid;
    private String numeroSocio;
    private Timestamp fecha;
}
