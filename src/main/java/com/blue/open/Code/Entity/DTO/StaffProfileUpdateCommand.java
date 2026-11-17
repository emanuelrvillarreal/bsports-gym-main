package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.SexoEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StaffProfileUpdateCommand {

    private String nombre;
    private String apellido;

    @Pattern(regexp = "^$|^\\d{7,11}$", message = "El DNI debe contener entre 7 y 11 dígitos numéricos.")
    private String dni;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private SexoEnum sexo;

    private String telefono;

    @Email(message = "Email inválido")
    private String email;
}
