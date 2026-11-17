package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class AmenityAltaCommand {

    @NotBlank(message = "Nombre requerido")
    @Size(max = 255, message = "Nombre demasiado largo")
    private String nombre;

    @NotNull(message = "Monto por hora requerido")
    @DecimalMin(value = "0", message = "El monto por hora no puede ser negativo")
    private BigDecimal montoHora;

    @NotNull(message = "Porcentaje de seña requerido")
    @DecimalMin(value = "0", message = "El porcentaje de seña no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de seña no puede superar 100")
    private BigDecimal porcentajeSena;

    @NotBlank(message = "Días de semana requeridos")
    @Size(max = 128, message = "Días de semana demasiado largos")
    private String diasSemana;

    @NotNull(message = "Hora de inicio requerida")
    private LocalTime horaInicio;

    @NotNull(message = "Hora de fin requerida")
    private LocalTime horaFin;

    @AssertTrue(message = "hora_fin debe ser posterior a hora_inicio")
    public boolean isHorarioValido() {
        if (horaInicio == null || horaFin == null) {
            return true;
        }
        return horaFin.isAfter(horaInicio);
    }

    @AssertTrue(message = "Amenity de pago requiere monto por hora mayor a cero y porcentaje de seña mayor a cero")
    public boolean isTarifaValida() {
        if (montoHora == null || porcentajeSena == null) {
            return true;
        }
        if (montoHora.compareTo(BigDecimal.ZERO) == 0) {
            return porcentajeSena.compareTo(BigDecimal.ZERO) == 0;
        }
        return montoHora.compareTo(BigDecimal.ZERO) > 0
                && porcentajeSena.compareTo(BigDecimal.ZERO) > 0;
    }
}
