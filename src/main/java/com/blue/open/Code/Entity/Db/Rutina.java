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

import com.blue.open.Code.Entity.Enums.RutinaEstadoEnum;

/** Plan de entrenamiento organizado por dias. Puede asignarse a uno o varios socios. */
@Setter
@Getter
@Table(name = "rutinas")
@Entity
public class Rutina extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "objetivo", columnDefinition = "TEXT")
    private String objetivo;

    @Column(name = "disciplina", length = 80)
    private String disciplina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Profesor responsable. Un profesor solo administra las rutinas a su cargo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por", nullable = false)
    private User creadaPor;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private RutinaEstadoEnum estado;

    /** Rutina anterior a la que esta reemplazo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reemplaza_a_id")
    private Rutina reemplazaA;
}
