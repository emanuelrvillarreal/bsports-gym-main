package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MemberBulkImportRow {

    private int rowNumber;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String password;
    private String telefono;
    private LocalDate fechaNacimiento;
    private com.blue.open.Code.Entity.Enums.SexoEnum sexo;
    private LocalDate fechaAlta;
    private Integer numeroSocio;
    private ParentescoEnum parentesco;
    private String titularDni;
    private List<String> warnings = new ArrayList<>();
}
