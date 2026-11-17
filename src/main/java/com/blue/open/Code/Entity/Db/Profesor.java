package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;

@Setter
@Getter
@Table(name = "profesores")
@Entity
public class Profesor extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private ProfesorEstadoEnum estado;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;
}
