package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Estadistica individual de un jugador del club en un partido. "Tantos" son goles, puntos o lo que use la disciplina. */
@Setter
@Getter
@Table(name = "partido_estadisticas")
@Entity
public class PartidoEstadistica extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private TorneoPartido partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private EquipoJugador jugador;

    @Column(name = "tantos", nullable = false)
    private Integer tantos = 0;

    @Column(name = "asistencias", nullable = false)
    private Integer asistencias = 0;

    @Column(name = "amonestaciones", nullable = false)
    private Integer amonestaciones = 0;

    @Column(name = "expulsiones", nullable = false)
    private Integer expulsiones = 0;

    @Column(name = "figura", nullable = false)
    private boolean figura;

    @Column(name = "observaciones", length = 255)
    private String observaciones;
}
