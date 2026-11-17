package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class InscripcionActividadDescuentoCommand {

    @NotBlank(message = "Descuento requerido")
    private String descuentoUuid;

    @NotNull(message = "Fecha desde requerida")
    private LocalDate fechaDesde;

    private LocalDate fechaHasta;
}
