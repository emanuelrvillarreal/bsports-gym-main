package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReservaPaginateRequestDTO {

    private int page = 1;
    private int size = 10;
    private List<ReservaEstadoEnum> estados = new ArrayList<>();
    private String amenityUuid;
    private String memberUuid;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime slotDesde;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime slotHasta;
    private String search;
}
