package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.blue.open.Code.Entity.Enums.PartidoEstadoEnum;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

/** Partido de un torneo, agrupado por ronda o fecha ("Fecha 1", "Semifinal"). */
@Setter
@Getter
@Table(name = "torneo_partidos")
@Entity
public class TorneoPartido extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @Column(name = "ronda", nullable = false, length = 60)
    private String ronda;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "lugar", length = 120)
    private String lugar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_id", nullable = false)
    private TorneoParticipante local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitante_id", nullable = false)
    private TorneoParticipante visitante;

    @Column(name = "tantos_local")
    private Integer tantosLocal;

    @Column(name = "tantos_visitante")
    private Integer tantosVisitante;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private PartidoEstadoEnum estado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
