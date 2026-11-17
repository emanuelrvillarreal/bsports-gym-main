package com.blue.open.Code.Entity.Db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Setter
@Getter
@Entity
@Table(name = "actividades_profesores")
@IdClass(ActividadProfesorId.class)
public class ActividadProfesor {

    @Id
    @Column(name = "actividad_id")
    private Long actividadId;

    @Id
    @Column(name = "profesor_id")
    private Long profesorId;

    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0", nullable = false)
    @JdbcTypeCode(Types.TINYINT)
    private Integer status = 0;
}
