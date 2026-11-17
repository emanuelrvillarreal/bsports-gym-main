package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/** Equipo o plantel competitivo del club. No es una actividad: puede apoyarse en una para el entrenamiento. */
@Setter
@Getter
@Table(name = "equipos_deportivos")
@Entity
public class EquipoDeportivo extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "disciplina", nullable = false, length = 80)
    private String disciplina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    @Column(name = "temporada", length = 40)
    private String temporada;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "equipos_deportivos_entrenadores",
            joinColumns = @JoinColumn(name = "equipo_id"),
            inverseJoinColumns = @JoinColumn(name = "profesor_id"))
    private Set<Profesor> entrenadores = new LinkedHashSet<>();
}
