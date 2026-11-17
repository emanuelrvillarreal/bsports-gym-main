package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AsistenciaPlanillaRowDTO {

    private String memberUuid;
    private String memberNombre;
    private String memberApellido;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaInscripcion;
    private AsistenciaActividadEstadoEnum estado;
}
