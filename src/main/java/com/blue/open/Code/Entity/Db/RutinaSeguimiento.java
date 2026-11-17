package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/** Sesion de entrenamiento registrada sobre una asignacion: que ejercicios hizo y como le fue. */
@Setter
@Getter
@Table(name = "rutina_seguimientos")
@Entity
public class RutinaSeguimiento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private RutinaAsignacion asignacion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Esfuerzo percibido de 1 a 10, opcional. */
    @Column(name = "esfuerzo")
    private Integer esfuerzo;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rutina_seguimiento_items",
            joinColumns = @JoinColumn(name = "seguimiento_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private Set<RutinaItem> itemsRealizados = new LinkedHashSet<>();
}
