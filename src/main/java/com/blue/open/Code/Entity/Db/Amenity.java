package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Setter
@Getter
@Table(name = "amenities")
@Entity
public class Amenity extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "monto_hora", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoHora;

    @Column(name = "porcentaje_sena", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeSena;

    @Column(name = "dias_semana", nullable = false, length = 128)
    private String diasSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
