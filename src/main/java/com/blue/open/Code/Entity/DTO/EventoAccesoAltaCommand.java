package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventoAccesoAltaCommand {

    @NotBlank(message = "Título requerido")
    @Size(max = 255, message = "Título demasiado largo")
    private String titulo;

    @NotBlank(message = "Descripción requerida")
    private String descripcion;

    @NotNull(message = "Monto requerido")
    @DecimalMin(value = "0.0", message = "El monto no puede ser negativo")
    private BigDecimal monto;

    private LocalDateTime fecha;

    @NotNull(message = "Debe indicar si permite morosos")
    private Boolean permiteMorosos;

    @NotNull(message = "Debe indicar si permite invitados")
    private Boolean permiteInvitados;

    @NotNull(message = "Monto para morosos requerido")
    @DecimalMin(value = "0.0", message = "El monto para morosos no puede ser negativo")
    private BigDecimal montoMorosos;

    @NotNull(message = "Monto para invitados requerido")
    @DecimalMin(value = "0.0", message = "El monto para invitados no puede ser negativo")
    private BigDecimal montoInvitados;
}
