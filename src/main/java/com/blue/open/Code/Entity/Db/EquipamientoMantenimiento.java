package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.MantenimientoTipoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;

/** Mantenimiento, reparacion o revision de un equipamiento. Si tuvo costo puede apuntar a su egreso. */
@Setter
@Getter
@Table(name = "equipamiento_mantenimientos")
@Entity
public class EquipamientoMantenimiento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamiento_id", nullable = false)
    private Equipamiento equipamiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private MantenimientoTipoEnum tipo;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "responsable", length = 120)
    private String responsable;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "costo", precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(name = "proximo_mantenimiento")
    private LocalDate proximoMantenimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    /** Egreso de tesoreria generado con el costo, si se pidio registrarlo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id")
    private Movimiento movimiento;
}
