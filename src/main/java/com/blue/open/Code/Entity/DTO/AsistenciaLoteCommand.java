package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AsistenciaLoteCommand {

    @NotEmpty(message = "El lote debe incluir al menos un ítem")
    @Valid
    private List<AsistenciaLoteItemCommand> items = new ArrayList<>();
}
