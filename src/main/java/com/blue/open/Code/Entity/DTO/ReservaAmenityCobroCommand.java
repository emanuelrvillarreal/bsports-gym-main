package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReservaAmenityCobroCommand {

    @NotNull(message = "Canal de cobro requerido")
    private CanalCobroEnum canalCobro;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private MedioPagoEnum medioPago;

    private LocalDate fecha;
    private String observaciones;

    @AssertTrue(message = "Monto y medio de pago requeridos para cobro manual")
    public boolean isManualValido() {
        if (canalCobro != CanalCobroEnum.MANUAL) {
            return true;
        }
        return monto != null && medioPago != null && medioPago != MedioPagoEnum.MERCADO_PAGO;
    }
}
