package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Setter
@Getter
@Table(name = "eventos_acceso_historial")
@Entity
public class EventoAccesoHistorial extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_acceso_id", nullable = false)
    private EventoAcceso eventoAcceso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private EventoAccesoHistorialTipoEnum tipo;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "resultado", nullable = false, length = 20)
    private EventoAccesoScanEstadoEnum resultado;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "monto", precision = 12, scale = 2)
    private BigDecimal monto;
}
