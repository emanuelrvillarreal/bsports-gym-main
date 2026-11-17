package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoAccesoCobroCommand {

    @NotNull(message = "Tipo requerido")
    private EventoAccesoHistorialTipoEnum tipo;

    @NotNull(message = "Payload requerido")
    private Object payload;
}
