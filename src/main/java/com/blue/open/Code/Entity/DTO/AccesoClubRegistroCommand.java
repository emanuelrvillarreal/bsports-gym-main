package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccesoClubRegistroCommand {

    @NotBlank(message = "Usuario requerido")
    private String userUuid;

    /** Opcional: si el ingreso es a una actividad, se controla que el pase del socio la incluya. */
    private String actividadUuid;

    /** Opcional: idem para una instalación (amenity). */
    private String amenityUuid;
}
