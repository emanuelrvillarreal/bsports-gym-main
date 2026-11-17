package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificadoMedicoResumenRequestDTO {

    private int page = 0;
    private int size = 10;
    /** Busca por nombre, apellido o DNI. */
    private String search;
    /** Si viene, solo los socios en esa situación. */
    private CertificadoSituacionEnum situacion;
    /** Si es true, solo los que están inscriptos en alguna actividad que exige certificado. */
    private boolean soloQueLoNecesitan;
}
