package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** Una fila del listado de certificados médicos del club: un socio y la situación de su certificado. */
@Getter
@Setter
public class CertificadoMedicoResumenFilaDTO {

    private String memberUuid;
    private String nombre;
    private String apellido;
    private String dni;
    private String imageUrl;
    private CertificadoSituacionEnum situacion;
    /** Último día de vigencia del certificado vigente (o del que venció más recientemente); null si no tiene. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaExpiracion;
    /** Días hasta el vencimiento si está vigente (puede ser negativo si ya venció). Null si no tiene fecha. */
    private Long diasRestantes;
    /** Actividades en las que está inscripto y que exigen certificado. */
    private List<String> actividadesQueLoExigen;
    /** True si alguna actividad lo exige y la situación no lo habilita: hoy está bloqueado en ella. */
    private boolean bloqueado;
}
