package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;

/** Item del inventario deportivo del club (pelotas, redes, maquinas, herramientas...). */
@Setter
@Getter
@Table(name = "equipamientos")
@Entity
public class Equipamiento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Codigo identificador interno (etiqueta, numero de inventario). Unico por club si se carga. */
    @Column(name = "codigo", length = 40)
    private String codigo;

    @Column(name = "tipo", length = 80)
    private String tipo;

    @Column(name = "disciplina", length = 80)
    private String disciplina;

    @Column(name = "marca", length = 80)
    private String marca;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EquipamientoEstadoEnum estado;

    @Column(name = "ubicacion", length = 120)
    private String ubicacion;

    @Column(name = "fecha_adquisicion")
    private LocalDate fechaAdquisicion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
