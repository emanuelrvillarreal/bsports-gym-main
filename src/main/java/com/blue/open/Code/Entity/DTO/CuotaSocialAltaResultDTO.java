package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CuotaSocialAltaResultDTO {

    private boolean generada;
    private boolean omitida;
    private String cuotaUuid;
    private String motivoOmitida;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate vencimiento;
    private CuotaEstadoEnum estado;
}
