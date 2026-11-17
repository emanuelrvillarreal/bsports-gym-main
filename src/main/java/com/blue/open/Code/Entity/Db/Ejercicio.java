package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.blue.open.Code.Entity.Enums.DificultadEjercicioEnum;
import com.blue.open.Code.Entity.Enums.TipoEjercicioEnum;

/** Ejercicio de la biblioteca del club, reutilizable en cualquier rutina. */
@Setter
@Getter
@Table(name = "ejercicios")
@Entity
public class Ejercicio extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "disciplina", length = 80)
    private String disciplina;

    /** Grupo muscular o habilidad que trabaja (ej. "Piernas", "Pase", "Salto"). */
    @Column(name = "grupo_muscular", length = 120)
    private String grupoMuscular;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoEjercicioEnum tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "dificultad", nullable = false, length = 20)
    private DificultadEjercicioEnum dificultad;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "indicaciones", columnDefinition = "TEXT")
    private String indicaciones;

    /** Quien lo cargo: solo esa persona (o un ADMIN) puede editarlo o borrarlo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private User creadoPor;
}
