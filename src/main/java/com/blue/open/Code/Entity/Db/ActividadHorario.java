package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalTime;

/**
 * Franja semanal en la que se dicta una actividad (ej. lunes 12:00 a 13:30). Una actividad tiene
 * uno o varios horarios; cada uno tiene su propio cupo.
 *
 * Relaciones: pertenece a una Actividad y a su Company. Las InscripcionActividad eligen uno o
 * varios horarios (tabla inscripciones_actividad_horarios) y cada Slot generado apunta al horario
 * del que salio, para que la planilla de asistencia y el calendario filtren por horario.
 */
@Setter
@Getter
@Table(name = "actividades_horarios")
@Entity
public class ActividadHorario extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "dia_semana", nullable = false, length = 12)
    private DiaSemanaEnum diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Cantidad maxima de inscripciones activas que pueden elegir este horario. */
    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;
}
