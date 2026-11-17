package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "categorias")
@Entity
public class Categoria extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 512)
    private String descripcion;

    /** Opcional: si es null, la categoría no tiene límite inferior de edad. */
    @Column(name = "edad_minima")
    private Integer edadMinima;

    /** Opcional: si es null, la categoría no tiene límite superior de edad. */
    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    public boolean admiteEdad(int edad) {
        if (edadMinima != null && edad < edadMinima) {
            return false;
        }
        return edadMaxima == null || edad <= edadMaxima;
    }
}
