package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReservaAmenityDTO {

    private String reservaUuid;
    private String amenityUuid;
    private String amenityNombre;
    private String memberUuid;
    private String memberNombre;
    private String memberApellido;
    private String slotUuid;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime slotInicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime slotFin;
    private SlotEstadoEnum slotEstado;
    private BigDecimal montoTotal;
    private BigDecimal montoSena;
    private BigDecimal saldoPendiente;
    private ReservaEstadoEnum estado;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime venceSenaAt;
    private String canceladaPorUuid;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime canceladaAt;
}
