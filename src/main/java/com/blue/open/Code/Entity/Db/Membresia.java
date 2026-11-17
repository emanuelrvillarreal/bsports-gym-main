package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaOrigenEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Un pase contratado por un socio (y, si es familiar, por los integrantes elegidos de su grupo). Se cobra con
 * una cuota de tipo PASE; renovaciones y cambios de plan crean una membresia nueva enlazada a la anterior,
 * asi el historial nunca se pisa.
 */
@Setter
@Getter
@Table(name = "membresias")
@Entity
public class Membresia extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pase_id", nullable = false)
    private Pase pase;

    /** Socio que contrata (en un pase familiar, el titular del grupo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_anterior_id")
    private Membresia membresiaAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "origen", nullable = false, length = 20)
    private MembresiaOrigenEnum origen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private MembresiaEstadoEnum estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Ultimo dia de vigencia (inclusive), ya extendido por congelamientos si corresponde. */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_fin_original", nullable = false)
    private LocalDate fechaFinOriginal;

    /** Copia de la cantidad del pase al contratar; null = ilimitado. */
    @Column(name = "ingresos_permitidos")
    private Integer ingresosPermitidos;

    @Column(name = "ingresos_usados", nullable = false)
    private int ingresosUsados;

    @Column(name = "renovacion_automatica", nullable = false)
    private boolean renovacionAutomatica;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    /** Integrantes cubiertos ademas del socio que contrata (solo pases familiares). */
    @ManyToMany
    @JoinTable(name = "membresias_integrantes", joinColumns = @JoinColumn(name = "membresia_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<Member> integrantes = new HashSet<>();

    @OneToMany(mappedBy = "membresia", fetch = FetchType.LAZY)
    private List<MembresiaCongelamiento> congelamientos = new ArrayList<>();

    public boolean cubreA(Long memberId) {
        return member.getId().equals(memberId) || integrantes.stream().anyMatch(i -> i.getId().equals(memberId));
    }

    public List<MembresiaCongelamiento> congelamientosActivos() {
        return congelamientos.stream().filter(c -> c.getStatus() == null || c.getStatus() == 0).toList();
    }

    public Integer ingresosDisponibles() {
        return ingresosPermitidos == null ? null : Math.max(0, ingresosPermitidos - ingresosUsados);
    }
}
