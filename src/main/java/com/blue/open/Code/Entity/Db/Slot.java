package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Setter
@Getter
@Table(name = "slots")
@Entity
public class Slot extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = true)
    private Actividad actividad;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private SlotEstadoEnum estado;

    /** Horario de la actividad del que se genero la clase; null para reservas de amenities. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id")
    private ActividadHorario horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;
}
