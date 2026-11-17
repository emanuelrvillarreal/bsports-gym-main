package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class AmenityDTO {

    private String amenityUuid;
    private String nombre;
    private BigDecimal montoHora;
    private BigDecimal porcentajeSena;
    private String diasSemana;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime horaInicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime horaFin;
}
