package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Setter
@Getter
@Table(name = "mercado_pago_traza")
@Entity
public class MercadoPagoTraza extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intent_id")
    private MercadoPagoCobroIntent intent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "etapa", nullable = false, length = 40)
    private MercadoPagoTrazaEtapaEnum etapa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "nivel", nullable = false, length = 10)
    private MercadoPagoTrazaNivelEnum nivel = MercadoPagoTrazaNivelEnum.INFO;

    @Column(name = "external_reference", length = 36, columnDefinition = "CHAR(36)")
    private String externalReference;

    @Column(name = "preference_id", length = 64)
    private String preferenceId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "mensaje", length = 512)
    private String mensaje;

    @Lob
    @Column(name = "request_raw", columnDefinition = "LONGTEXT")
    private String requestRaw;

    @Lob
    @Column(name = "response_raw", columnDefinition = "LONGTEXT")
    private String responseRaw;
}
