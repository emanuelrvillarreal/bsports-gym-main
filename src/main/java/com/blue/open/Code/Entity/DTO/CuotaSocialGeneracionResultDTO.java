package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CuotaSocialGeneracionResultDTO {

    /** Fecha hasta la que se generaron las cuotas pendientes. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate hasta;
    private int companiesProcessed;
    private int cuotasGeneradas;
    /** Socios que correspondía facturar y no se pudo (socio sin titular o sin rango de cuota social). */
    private int cuotasOmitidas;

    @Override
    public String toString() {
        return "CuotaSocialGeneracionResult{hasta=" + hasta
                + ", companies=" + companiesProcessed
                + ", generadas=" + cuotasGeneradas
                + ", omitidas=" + cuotasOmitidas + "}";
    }
}
