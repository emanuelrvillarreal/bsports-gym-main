package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Entity.Enums.SexoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class MemberSolicitudRegistroDTO {

    private String memberUuid;
    private MemberRegistroTipoEnum tipoRegistro;
    private String titularMemberUuid;
    private String titularNombre;
    private ParentescoEnum parentesco;
    private String numeroSocio;

    private String nombre;
    private String apellido;
    private String dni;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;
    private SexoEnum sexo;
    private String telefono;
    private String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fechaSolicitud;
}
