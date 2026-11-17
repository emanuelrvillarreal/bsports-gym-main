package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaginateFiltro {
    private String tabla = "";
    private String columna = "";
    private String operacion = "";
    private String valor = "";

    public PaginateFiltro(String tabla, String columna, String valor) {
        this.tabla = tabla;
        this.columna = columna;
        this.valor = valor;
        this.operacion = "=";
    }

    public PaginateFiltro(String columna, String valor) {
        this.tabla = "t";
        this.columna = columna;
        this.operacion = "=";
        this.valor = valor;
    }

    public PaginateFiltro(String tabla, String columna, String operacion, String valor) {
        this.tabla = tabla;
        this.columna = columna;
        this.operacion = operacion;
        this.valor = valor;
    }
}
