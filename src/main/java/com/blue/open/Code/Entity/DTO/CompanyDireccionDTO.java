package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyDireccionDTO {

    @NotBlank(message = "La dirección es obligatoria.")
    private String direccion;

    @NotBlank(message = "El código postal es obligatorio.")
    private String codigoPostal;

    @NotBlank(message = "La localidad es obligatoria.")
    private String localidad;
}
