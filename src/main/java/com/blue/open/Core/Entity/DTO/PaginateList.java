package com.blue.open.Core.Entity.DTO;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PaginateList {
    private int page = 0;
    private int size = 10;

    @JsonIgnore
    private String table;
    @JsonIgnore
    private Class<?> entityClass;

    private List<PaginateParametro> parametros = new ArrayList<>();
    private List<List<PaginateFiltro>> filtros = new ArrayList<>();
    private List<PaginateRelacion> relaciones = new ArrayList<>();
    private List<PaginateOrden> ordenes = new ArrayList<>();

    public PaginateList() {
    }

    public PaginateList(String table, Class<? extends AbstractEntity> entityClass) {
        this.table = table;
        this.entityClass = entityClass;
    }

    public void init(String table) {
        this.table = table;
    }

    public boolean isInitialized() {
        return this.table != null && !this.table.isBlank();
    }

    public PaginateParametro getParametroByNombre(String nombre) {
        for (PaginateParametro p : this.parametros) {
            if (p.getNombre().equalsIgnoreCase(nombre)) {
                return p;
            }
        }
        return null;
    }

    public void addParametro(PaginateParametro parametro) {
        this.parametros.add(parametro);
    }

    public void addFiltro(List<PaginateFiltro> filtro) {
        this.filtros.add(filtro);
    }
}
