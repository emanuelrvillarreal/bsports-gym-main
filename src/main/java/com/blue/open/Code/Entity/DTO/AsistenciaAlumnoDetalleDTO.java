package com.blue.open.Code.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Detalle de asistencia de un alumno en una actividad: totales, evolucion mensual y cada clase. */
@Getter
@Setter
public class AsistenciaAlumnoDetalleDTO {

    private AsistenciaAlumnoFilaDTO alumno;
    private List<AsistenciaMesDTO> evolucionMensual = new ArrayList<>();
    /** Clases del periodo, de la mas nueva a la mas vieja. */
    private List<Clase> clases = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Clase {
        private LocalDateTime inicio;
        /** PRESENTE, AUSENTE o AUSENTE_JUSTIFICADO. Sin registro en una planilla cargada cuenta como AUSENTE. */
        private String estado;
    }
}
