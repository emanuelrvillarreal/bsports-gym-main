package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CalendarioSlotDTO {

    private String slotUuid;
    private String eventoUuid;
    private String titulo;
    private String ubicacion;
    private CalendarioSlotTipoEnum tipo;
    private String referenciaUuid;
    private String referenciaNombre;
    /** Horario de la actividad del que sale la clase; null en eventos y reservas. */
    private String horarioUuid;
    private String titularNombre;
    private String titularApellido;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime inicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fin;
    private SlotEstadoEnum estado;
    /** Porcentaje (0-100) de alumnos con asistencia PRESENTE. Solo en slots de actividad del calendario del profesor. */
    private Double porcentajeAsistencia;
}
