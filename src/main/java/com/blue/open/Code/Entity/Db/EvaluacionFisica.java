package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evaluacion fisica de un socio en una fecha. Las sucesivas forman su historial; nunca se pisan.
 * Son mediciones de seguimiento deportivo, no evaluaciones clinicas.
 */
@Setter
@Getter
@Table(name = "evaluaciones_fisicas")
@Entity
public class EvaluacionFisica extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Usuario que la cargo en el sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrada_por", nullable = false)
    private User registradaPor;

    /** Profesional responsable, si no es quien la carga (ej. "Lic. Gomez"). */
    @Column(name = "profesional", length = 120)
    private String profesional;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "altura_cm", precision = 5, scale = 1)
    private BigDecimal alturaCm;

    /** Calculado al guardar: peso / (altura en metros)^2. */
    @Column(name = "imc", precision = 5, scale = 2)
    private BigDecimal imc;

    @Column(name = "grasa_corporal_pct", precision = 4, scale = 1)
    private BigDecimal grasaCorporalPct;

    @Column(name = "masa_muscular_kg", precision = 5, scale = 2)
    private BigDecimal masaMuscularKg;

    @Column(name = "cintura_cm", precision = 5, scale = 1)
    private BigDecimal cinturaCm;

    @Column(name = "cadera_cm", precision = 5, scale = 1)
    private BigDecimal caderaCm;

    @Column(name = "pecho_cm", precision = 5, scale = 1)
    private BigDecimal pechoCm;

    @Column(name = "brazo_cm", precision = 5, scale = 1)
    private BigDecimal brazoCm;

    @Column(name = "muslo_cm", precision = 5, scale = 1)
    private BigDecimal musloCm;

    @Column(name = "pantorrilla_cm", precision = 5, scale = 1)
    private BigDecimal pantorrillaCm;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "recomendaciones", columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "proxima_evaluacion")
    private LocalDate proximaEvaluacion;
}
