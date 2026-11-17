package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaEdicionCommand {

    @NotBlank(message = "Nombre requerido")
    @Size(max = 255, message = "Nombre demasiado largo")
    private String nombre;

    @Size(max = 512, message = "Descripción demasiado larga")
    private String descripcion;

    /** Opcional: si no viene, la categoría no tiene límite inferior de edad. */
    @Min(value = 0, message = "Edad mínima inválida")
    @Max(value = 120, message = "Edad mínima inválida")
    private Integer edadMinima;

    /** Opcional: si no viene, la categoría no tiene límite superior de edad. */
    @Min(value = 0, message = "Edad máxima inválida")
    @Max(value = 120, message = "Edad máxima inválida")
    private Integer edadMaxima;

    @AssertTrue(message = "edad_maxima debe ser mayor o igual a edad_minima")
    public boolean isRangoEdadValido() {
        if (edadMinima == null || edadMaxima == null) {
            return true;
        }
        return edadMaxima >= edadMinima;
    }
}
