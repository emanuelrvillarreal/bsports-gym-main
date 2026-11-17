package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.SexoEnum;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemberPortalProfileCommand {

    private String nombre;
    private String apellido;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private SexoEnum sexo;

    private String telefono;
}
