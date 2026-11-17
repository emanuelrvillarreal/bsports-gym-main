package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsistenciaLoteItemCommand {

    @NotBlank(message = "Member requerido")
    private String memberUuid;

    @NotNull(message = "Estado requerido")
    private AsistenciaActividadEstadoEnum estado;
}
