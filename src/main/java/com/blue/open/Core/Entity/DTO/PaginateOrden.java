package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginateOrden {
    private String tabla = "t";
    private String columna;
    private String direccion; //asc or desc
}
