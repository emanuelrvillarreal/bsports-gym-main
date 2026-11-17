package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Entity.Enums.SexoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfesorDTO {

    private String profesorUuid;
    private ProfesorEstadoEnum estado;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaAlta;

    private String nombre;
    private String apellido;
    private String dni;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;
    private SexoEnum sexo;
    private String telefono;
    private String email;
    private String imageUrl;
}
