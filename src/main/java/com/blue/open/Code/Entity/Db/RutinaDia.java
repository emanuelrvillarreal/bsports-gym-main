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

/** Dia (o bloque) de una rutina con sus ejercicios en orden. */
@Setter
@Getter
@Table(name = "rutina_dias")
@Entity
public class RutinaDia extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;
}
