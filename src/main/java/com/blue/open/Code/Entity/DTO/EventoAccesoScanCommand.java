package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoAccesoScanCommand {

    @NotBlank(message = "Usuario requerido")
    private String userUuid;
}
