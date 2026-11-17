package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Setter
@Getter
@Table(name = "mercado_pago_cobro_intent")
@Entity
public class MercadoPagoCobroIntent extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "external_reference", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String externalReference;

    @Column(name = "preference_id", nullable = false, length = 64)
    private String preferenceId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private MercadoPagoCobroIntentEstadoEnum estado = MercadoPagoCobroIntentEstadoEnum.PENDIENTE;

    @OneToOne(mappedBy = "mercadoPagoCobroIntent", fetch = FetchType.LAZY)
    private MercadoPagoCobro mercadoPagoCobro;
}
