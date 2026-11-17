package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Setter
@Getter
@Table(name = "member_conversiones")
@Entity
public class MemberConversion extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_anterior_id", nullable = false)
    private Member titularAnterior;

    @Column(name = "nuevo_numero_socio", nullable = false, length = 20)
    private String nuevoNumeroSocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejecutado_por", nullable = false)
    private User ejecutadoPor;

    @Column(name = "fecha", nullable = false)
    private Timestamp fecha;
}
