package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventoAccesoDetalleDTO {

    private String uuid;
    private String titulo;
    private String descripcion;
    private BigDecimal monto;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDateTime fecha;
    private Boolean permiteMorosos;
    private Boolean permiteInvitados;
    private BigDecimal montoMorosos;
    private BigDecimal montoInvitados;
}
