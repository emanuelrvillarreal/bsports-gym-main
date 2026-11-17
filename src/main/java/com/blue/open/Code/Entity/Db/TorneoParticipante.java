package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Participante de un torneo: un equipo del club o un rival externo (solo nombre). */
@Setter
@Getter
@Table(name = "torneo_participantes")
@Entity
public class TorneoParticipante extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    /** Null para rivales externos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id")
    private EquipoDeportivo equipo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;
}
