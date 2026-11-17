package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Horario que se envia al crear o editar una actividad. Sin uuid se crea uno nuevo; con uuid se
 * actualiza el existente. Los horarios que no se envian al editar se dan de baja.
 */
@Getter
@Setter
public class ActividadHorarioCommand {

    @Size(max = 36, message = "UUID de horario inválido")
    private String uuid;

    @NotNull(message = "Día del horario requerido")
    private DiaSemanaEnum diaSemana;

    @NotNull(message = "Hora de inicio del horario requerida")
    private LocalTime horaInicio;

    @NotNull(message = "Hora de fin del horario requerida")
    private LocalTime horaFin;

    @NotNull(message = "Cupo del horario requerido")
    @Min(value = 1, message = "El cupo del horario debe ser al menos 1")
    @Max(value = 10000, message = "El cupo del horario es demasiado grande")
    private Integer cupoMaximo;

    @AssertTrue(message = "La hora de fin del horario debe ser posterior a la de inicio")
    public boolean isFranjaValida() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
