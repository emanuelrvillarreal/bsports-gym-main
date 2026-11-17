package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;

@Setter
@Getter
@Table(name = "certificados_medicos")
@Entity
public class CertificadoMedico extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDate fechaCarga;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDate fechaExpiracion;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "url_archivo", nullable = false, length = 512)
    private String urlArchivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 30)
    private CertificadoMedicoEstadoEnum estado;
}
