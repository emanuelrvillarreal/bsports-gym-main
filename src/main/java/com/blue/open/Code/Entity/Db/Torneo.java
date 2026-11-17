package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.blue.open.Code.Entity.Enums.TorneoEstadoEnum;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;

/** Torneo o campeonato. Los puntos por resultado son configurables segun la disciplina. */
@Setter
@Getter
@Table(name = "torneos")
@Entity
public class Torneo extends AbstractEntity {

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

    @Column(name = "temporada", length = 40)
    private String temporada;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private TorneoEstadoEnum estado;

    @Column(name = "puntos_victoria", nullable = false)
    private Integer puntosVictoria = 3;

    @Column(name = "puntos_empate", nullable = false)
    private Integer puntosEmpate = 1;

    @Column(name = "puntos_derrota", nullable = false)
    private Integer puntosDerrota = 0;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
