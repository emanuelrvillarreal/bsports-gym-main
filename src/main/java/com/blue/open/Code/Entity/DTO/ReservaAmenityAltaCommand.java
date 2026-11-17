package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservaAmenityAltaCommand {

    @NotBlank(message = "Amenity requerido")
    private String amenityUuid;

    @NotBlank(message = "Socio titular requerido")
    private String memberUuid;

    @NotNull(message = "Inicio del slot requerido")
    private LocalDateTime inicio;

    @NotNull(message = "Fin del slot requerido")
    private LocalDateTime fin;
}
