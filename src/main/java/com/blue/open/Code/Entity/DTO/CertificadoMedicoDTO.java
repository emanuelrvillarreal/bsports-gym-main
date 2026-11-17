package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CertificadoMedicoDTO {

    private String certificadoUuid;
    private String memberUuid;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaCarga;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaExpiracion;
    private String nombreArchivo;
    private String urlArchivo;
    private CertificadoMedicoEstadoEnum estado;
}
