package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CuotaActividadAltaResultDTO {

    private boolean generada;
    /** No se generó porque el socio tiene un pase vigente que incluye la cuota de la actividad. */
    private boolean cubiertaPorPase;
    /** No se generó porque ya tenía la cuota de ese período (se reinscribió en el mismo período). */
    private boolean yaExistia;
    /** Inscripción después del día de corte: no se generó cuota ahora; la primera se genera en esta fecha. */
    private LocalDate primeraCuota;
    private String cuotaUuid;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate vencimiento;
    private CuotaEstadoEnum estado;
}
