package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ActividadInscriptoDTO {

    private String inscripcionUuid;
    /** Horarios de la actividad que eligio el socio. */
    private List<String> horarioUuids;
    private String userUuid;
    private String titularMemberUuid;
    private String nombre;
    private String apellido;
    private String imageUrl;
    private String email;
    private String dni;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;
    /** Solo si la actividad exige certificado médico; null si no lo exige. */
    private com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum certificadoSituacion;
}
