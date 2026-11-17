package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CuotaSocialRangoEdicionCommand {

    @NotBlank(message = "Nombre requerido")
    @Size(max = 255, message = "Nombre demasiado largo")
    private String nombre;

    @Size(max = 512, message = "Descripción demasiado larga")
    private String descripcion;

    @NotNull(message = "Edad hasta requerida")
    @Min(value = 0, message = "Edad hasta inválida")
    @Max(value = 120, message = "Edad hasta inválida")
    private Integer edadHasta;

    @NotNull(message = "Importe requerido")
    @DecimalMin(value = "0.0", message = "El importe no puede ser negativo")
    private BigDecimal importe;
}
