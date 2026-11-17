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

/** Ejercicio dentro de un dia de la rutina, con su prescripcion. */
@Setter
@Getter
@Table(name = "rutina_items")
@Entity
public class RutinaItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dia_id", nullable = false)
    private RutinaDia dia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "series")
    private Integer series;

    /** Texto libre: "12", "8-10", "30 seg", "hasta el fallo". */
    @Column(name = "repeticiones", length = 40)
    private String repeticiones;

    /** Peso o intensidad: "20 kg", "RPE 7", "70%". */
    @Column(name = "carga", length = 60)
    private String carga;

    @Column(name = "descanso_segundos")
    private Integer descansoSegundos;

    @Column(name = "observaciones", length = 500)
    private String observaciones;
}
