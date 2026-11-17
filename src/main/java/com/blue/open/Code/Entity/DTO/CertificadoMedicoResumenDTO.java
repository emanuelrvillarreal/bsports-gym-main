package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Map;

@Getter
@Setter
public class CertificadoMedicoResumenDTO {

    /** Cantidad de socios por situación, sobre todo el club (sin aplicar los filtros del pedido). */
    private Map<CertificadoSituacionEnum, Long> conteos;
    /** Socios bloqueados hoy en alguna actividad por no tener certificado vigente. */
    private long bloqueados;
    private Page<CertificadoMedicoResumenFilaDTO> filas;
}
