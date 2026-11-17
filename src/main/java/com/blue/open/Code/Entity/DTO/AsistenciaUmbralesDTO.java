package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Umbrales del semaforo de asistencia y de las alertas de inasistencia, configurables por club. */
@Getter
@Setter
public class AsistenciaUmbralesDTO {

    /** Desde este porcentaje la asistencia es buena (verde). */
    @Min(1) @Max(100)
    private int verde = 80;

    /** Desde este porcentaje y por debajo de verde es regular (amarillo); por debajo, rojo. */
    @Min(0) @Max(100)
    private int amarillo = 60;

    /** Alerta cuando el porcentaje queda por debajo de este valor. */
    @Min(0) @Max(100)
    private int porcentajeAlerta = 50;

    /** Alerta cuando el alumno lleva estos dias sin presentes habiendo tenido clases. */
    @Min(1) @Max(365)
    private int diasSinAsistencia = 15;

    /** Alerta con esta cantidad de ausencias seguidas (las justificadas no cuentan). */
    @Min(1) @Max(50)
    private int ausenciasConsecutivas = 3;
}
