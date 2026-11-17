package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Horario de una actividad tal como lo ven el admin y el socio: franja, cupo y cuantos lugares
 * quedan. {@code disponibles} es 0 cuando el horario esta lleno.
 */
@Getter
@Setter
public class ActividadHorarioDTO {
    private String uuid;
    private DiaSemanaEnum diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer cupoMaximo;
    private long inscriptos;
    private long disponibles;
}
