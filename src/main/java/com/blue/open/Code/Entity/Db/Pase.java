package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.PaseCategoriaEnum;
import com.blue.open.Code.Entity.Enums.PaseModalidadEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tipo de pase que ofrece un club. Sin actividades ni instalaciones cargadas, el pase es de acceso general;
 * sin dias ni horario, vale todos los dias a toda hora; sin cantidad de ingresos, es ilimitado.
 */
@Setter
@Getter
@Table(name = "pases")
@Entity
public class Pase extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "categoria", nullable = false, length = 20)
    private PaseCategoriaEnum categoria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "modalidad", nullable = false, length = 20)
    private PaseModalidadEnum modalidad;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** null = ingresos ilimitados. */
    @Column(name = "cantidad_ingresos")
    private Integer cantidadIngresos;

    /** Dias de la semana separados por coma (LUNES,MARTES...). null = todos. */
    @Column(name = "dias_permitidos", length = 80)
    private String diasPermitidos;

    @Column(name = "hora_desde")
    private LocalTime horaDesde;

    @Column(name = "hora_hasta")
    private LocalTime horaHasta;

    @Column(name = "renovacion_automatica", nullable = false)
    private boolean renovacionAutomatica;

    @Column(name = "congelamiento_extiende", nullable = false)
    private boolean congelamientoExtiende = true;

    /** Si el socio tiene este pase vigente, no se le cobran las cuotas de las actividades que cubre. */
    @Column(name = "incluye_cuota_actividades", nullable = false)
    private boolean incluyeCuotaActividades;

    @Column(name = "familiar", nullable = false)
    private boolean familiar;

    @Column(name = "max_integrantes")
    private Integer maxIntegrantes;

    @Column(name = "venta_portal", nullable = false)
    private boolean ventaPortal;

    @Column(name = "venta_desde")
    private LocalDate ventaDesde;

    @Column(name = "venta_hasta")
    private LocalDate ventaHasta;

    @Column(name = "restricciones", columnDefinition = "TEXT")
    private String restricciones;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @ManyToMany
    @JoinTable(name = "pases_actividades", joinColumns = @JoinColumn(name = "pase_id"),
            inverseJoinColumns = @JoinColumn(name = "actividad_id"))
    private Set<Actividad> actividades = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "pases_amenities", joinColumns = @JoinColumn(name = "pase_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new HashSet<>();

    /** Descuentos del catalogo que se pueden aplicar al vender este pase. */
    @ManyToMany
    @JoinTable(name = "pases_descuentos", joinColumns = @JoinColumn(name = "pase_id"),
            inverseJoinColumns = @JoinColumn(name = "descuento_id"))
    private Set<Descuento> descuentos = new HashSet<>();

    public Set<DiaSemanaEnum> dias() {
        if (diasPermitidos == null || diasPermitidos.isBlank()) {
            return EnumSet.allOf(DiaSemanaEnum.class);
        }
        return Arrays.stream(diasPermitidos.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(DiaSemanaEnum::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DiaSemanaEnum.class)));
    }

    public void definirDias(Set<DiaSemanaEnum> dias) {
        if (dias == null || dias.isEmpty() || dias.size() == DiaSemanaEnum.values().length) {
            this.diasPermitidos = null;
            return;
        }
        this.diasPermitidos = EnumSet.copyOf(dias).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public boolean seVendeEl(LocalDate fecha) {
        return activo && (ventaDesde == null || !fecha.isBefore(ventaDesde))
                && (ventaHasta == null || !fecha.isAfter(ventaHasta));
    }
}
