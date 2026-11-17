package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Suspension temporal de una membresia. Si el pase lo indica, los dias corren el vencimiento. */
@Setter
@Getter
@Table(name = "membresias_congelamientos")
@Entity
public class MembresiaCongelamiento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = false)
    private Membresia membresia;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "dias", nullable = false)
    private int dias;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "extiende_vencimiento", nullable = false)
    private boolean extiendeVencimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_por", nullable = false)
    private User autorizadoPor;

    public boolean cubre(LocalDate fecha) {
        return !fecha.isBefore(fechaDesde) && !fecha.isAfter(fechaHasta);
    }
}
