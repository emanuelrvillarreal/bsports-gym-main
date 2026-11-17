package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Periodo de un socio en un plantel. La baja o el pase cierran el periodo; nunca se borra. */
@Setter
@Getter
@Table(name = "equipo_jugadores")
@Entity
public class EquipoJugador extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private EquipoDeportivo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "dorsal")
    private Integer dorsal;

    @Column(name = "posicion", length = 60)
    private String posicion;

    @Column(name = "capitan", nullable = false)
    private boolean capitan;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    public boolean activo() {
        return fechaBaja == null;
    }
}
