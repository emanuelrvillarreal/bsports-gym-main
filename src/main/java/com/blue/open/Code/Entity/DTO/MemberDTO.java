package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Entity.Enums.SexoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MemberDTO {

    private String memberUuid;
    private String userUuid;
    private boolean titular;
    private boolean tieneFamiliares;
    private String titularMemberUuid;
    private ParentescoEnum parentesco;
    private String numeroSocio;
    private MemberEstadoEnum estado;
    private int status;
    private MemberCuotaSocialRangoDTO cuotaSocialRango;
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

    private List<CertificadoMedicoDTO> certificados;
}
