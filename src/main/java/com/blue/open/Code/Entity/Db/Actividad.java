package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Table(name = "actividades")
@Entity
public class Actividad extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "ubicacion", nullable = false)
    private String ubicacion;

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;

    @Column(name = "dias_semana", nullable = false, length = 128)
    private String diasSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "importe_cuota", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeCuota;

    /** Cada cuánto se genera la cuota de la actividad. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "periodicidad_cuota", nullable = false, length = 20)
    private PeriodicidadCuotaEnum periodicidadCuota = PeriodicidadCuotaEnum.MENSUAL;

    @Column(name = "dias_vencimiento_cuota", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 30")
    private Integer diasVencimientoCuota = 30;

    /**
     * Dia del mes (1 a 28) en que vencen las cuotas. Si esta informado, la primera cuota vence el
     * proximo dia indicado y las siguientes se generan el 1 de cada periodo; si es null, cada cuota
     * vence {@link #diasVencimientoCuota} dias despues de generada.
     */
    @Column(name = "dia_vencimiento_fijo")
    private Integer diaVencimientoFijo;

    public boolean tieneDiaVencimientoFijo() {
        return diaVencimientoFijo != null;
    }

    /**
     * Dia de corte de inscripcion (1 a 28; solo con dia fijo de vencimiento). Quien se inscribe despues de ese
     * dia no paga el mes en curso: su primera cuota es la del mes siguiente. Null: se cobra el mes de la inscripcion.
     */
    @Column(name = "dia_corte_inscripcion")
    private Integer diaCorteInscripcion;

    /** La inscripcion de {@code fecha} cae despues del dia de corte: su primera cuota es la del mes siguiente. */
    public boolean inscripcionPasaAlMesSiguiente(java.time.LocalDate fecha) {
        return tieneDiaVencimientoFijo() && diaCorteInscripcion != null && fecha != null
                && fecha.getDayOfMonth() > diaCorteInscripcion;
    }

    /** Si es true, el socio necesita un certificado médico vigente para inscribirse y para que se le tome asistencia. */
    @Column(name = "requiere_certificado", nullable = false)
    private boolean requiereCertificado;

    /** Recargo por pago fuera de término de las cuotas de esta actividad. Apagado por defecto. */
    @Embedded
    private RecargoConfig recargo = new RecargoConfig();

    public RecargoConfig getRecargo() {
        if (recargo == null) {
            recargo = new RecargoConfig();
        }
        return recargo;
    }

    @Column(name = "completa", nullable = false, columnDefinition = "BIT(1) NOT NULL DEFAULT 0")
    private boolean completa;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "actividades_categorias",
            joinColumns = @JoinColumn(name = "actividad_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    @BatchSize(size = 50)
    private Set<Categoria> categorias = new LinkedHashSet<>();
}
