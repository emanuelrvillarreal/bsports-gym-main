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

/** Registro de cada cambio de una rutina (alta, edicion, asignacion, finalizacion, reemplazo). */
@Setter
@Getter
@Table(name = "rutina_historial")
@Entity
public class RutinaHistorial extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "fecha", nullable = false)
    private java.sql.Timestamp fecha;

    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "detalle", length = 500)
    private String detalle;
}
