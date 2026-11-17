package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;

@Setter
@Getter
@Table(name = "members")
@Entity
public class Member extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id")
    private Member titular;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "parentesco", length = 30)
    private ParentescoEnum parentesco;

    @Column(name = "numero_socio", length = 20)
    private String numeroSocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_social_rango_id")
    private CuotaSocialRango cuotaSocialRango;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private MemberEstadoEnum estado;

    @Column(name = "grupo_inhabilitado_at")
    private Timestamp grupoInhabilitadoAt;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    /** Fecha en la que le toca la próxima cuota social. La avanza {@link #avanzarProximaGeneracionCuota()}. */
    @Column(name = "proxima_generacion_cuota", nullable = false)
    private LocalDate proximaGeneracionCuota;

    public boolean isTitular() {
        return titular == null;
    }

    /** Corre la próxima generación de cuota social un período, según la periodicidad del club. */
    public void avanzarProximaGeneracionCuota() {
        this.proximaGeneracionCuota = company.getPeriodicidadCuotaSocial().siguienteFecha(proximaGeneracionCuota);
    }

    public Long resolveTitularMemberId() {
        if (isTitular()) {
            return getId();
        }
        return titular.getId();
    }
}
