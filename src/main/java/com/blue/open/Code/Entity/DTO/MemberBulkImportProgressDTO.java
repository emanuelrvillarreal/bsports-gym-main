package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberBulkImportProgressDTO {

    private int filasProcesadas;
    private int filasTotales;
    private int porcentajeCompletado;
    private int errorCount;
    private int warningCount;
    private String errorMessage;
}
