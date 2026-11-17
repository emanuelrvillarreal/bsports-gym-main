package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MovimientoPaginateRequestDTO {

    private int page = 1;
    private int size = 20;
    private MovimientoTipoEnum tipo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private List<MovimientoEstadoEnum> estados = new ArrayList<>();
}
