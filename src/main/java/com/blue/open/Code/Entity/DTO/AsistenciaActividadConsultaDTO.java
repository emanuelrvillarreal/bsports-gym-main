package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AsistenciaActividadConsultaDTO {

    private String uuid;
    private String slotUuid;
    private String actividadUuid;
    private String actividadNombre;
    private String memberUuid;
    private String memberNombre;
    private String memberApellido;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fecha;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime slotInicio;
    private AsistenciaActividadEstadoEnum estado;
}
