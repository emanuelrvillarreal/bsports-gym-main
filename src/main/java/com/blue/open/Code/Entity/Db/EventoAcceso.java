package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Table(name = "eventos_acceso")
@Entity
public class EventoAcceso extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = true)
    private LocalDateTime fecha;

    @Column(name = "permite_morosos", nullable = false)
    private Boolean permiteMorosos;

    @Column(name = "permite_invitados", nullable = false)
    private Boolean permiteInvitados;

    @Column(name = "monto_morosos", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMorosos;

    @Column(name = "monto_invitados", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInvitados;
}
