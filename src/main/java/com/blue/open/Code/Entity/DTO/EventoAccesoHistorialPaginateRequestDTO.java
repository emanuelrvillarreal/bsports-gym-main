package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoAccesoHistorialPaginateRequestDTO {

    private int page = 1;
    private int size = 10;
    private String search;
    private EventoAccesoScanEstadoEnum resultado;
}
