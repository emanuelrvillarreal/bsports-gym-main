package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.SexoEnum;
import com.blue.open.Core.Entity.Db.BaseUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;

@Setter
@Getter
@Table(name = "auth_users")
@Entity()
public class User extends BaseUser {

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "dni", length = 64, unique = true)
    private String dni;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "sexo", length = 20)
    private SexoEnum sexo;

    @Column(name = "image_url", length = 512)
    private String imageUrl;
}
