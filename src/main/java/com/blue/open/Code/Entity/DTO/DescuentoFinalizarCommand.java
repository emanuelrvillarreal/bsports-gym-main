package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DescuentoFinalizarCommand {

    @NotNull(message = "Fecha requerida")
    private LocalDate fecha;
}
