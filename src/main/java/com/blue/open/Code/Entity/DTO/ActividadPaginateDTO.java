package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ActividadPaginateDTO {

    private String actividadUuid;
    private String nombre;
    private String ubicacion;
    private Integer cupoMaximo;
    private String diasSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal importeCuota;
    private boolean completa;
    private List<String> categoriaNombres;
    private long cantidadInscriptos;

    /** Franjas de la actividad con su cupo disponible; los campos de horario de arriba son un resumen. */
    private List<ActividadHorarioDTO> horarios;
}
