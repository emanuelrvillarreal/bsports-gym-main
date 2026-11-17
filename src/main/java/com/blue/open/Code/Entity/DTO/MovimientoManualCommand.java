package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MovimientoManualCommand {

    @NotNull(message = "Tipo de movimiento requerido")
    private MovimientoTipoEnum tipo;

    @NotNull(message = "Monto requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate fecha;

    @NotBlank(message = "Concepto requerido")
    @Size(max = 255, message = "Concepto demasiado largo")
    private String concepto;

    private MedioPagoEnum medioPago;

    @Size(max = 65535, message = "Observaciones demasiado largas")
    private String observaciones;
}
