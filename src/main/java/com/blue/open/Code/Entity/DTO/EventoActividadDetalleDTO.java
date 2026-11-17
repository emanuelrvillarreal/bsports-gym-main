package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventoActividadDetalleDTO {

    private String uuid;
    private String actividadUuid;
    private String actividadNombre;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime inicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fin;
    private EventoActividadEstadoEnum estado;
}
