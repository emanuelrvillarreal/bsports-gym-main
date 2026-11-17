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

import com.blue.open.Code.Entity.Enums.AsignacionRutinaEstadoEnum;

/** Rutina asignada a un socio. Al finalizar o reemplazar queda en su historial. */
@Setter
@Getter
@Table(name = "rutina_asignaciones")
@Entity
public class RutinaAsignacion extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignada_por", nullable = false)
    private User asignadaPor;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_finalizacion")
    private LocalDate fechaFinalizacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private AsignacionRutinaEstadoEnum estado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
