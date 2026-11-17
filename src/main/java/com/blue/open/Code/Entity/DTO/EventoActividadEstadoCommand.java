package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoActividadEstadoCommand {

    @NotNull(message = "Estado requerido")
    private EventoActividadEstadoEnum estado;
}
