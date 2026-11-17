package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaDTO {

    private String categoriaUuid;
    private String nombre;
    private String descripcion;
    private Integer edadMinima;
    private Integer edadMaxima;
}
