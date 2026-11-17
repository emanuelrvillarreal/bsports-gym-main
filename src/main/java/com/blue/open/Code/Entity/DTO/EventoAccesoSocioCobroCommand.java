package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EventoAccesoSocioCobroCommand {

    @NotBlank(message = "Usuario requerido")
    private String userUuid;

    @NotNull(message = "Canal de cobro requerido")
    private CanalCobroEnum canalCobro;

    @Min(value = 1, message = "Cantidad de entradas debe ser al menos 1")
    private Integer cantidadEntradas;

    private MedioPagoEnum medioPago;

    private LocalDate fecha;

    private String observaciones;

    @AssertTrue(message = "Medio de pago requerido para cobro manual")
    public boolean isMedioPagoValidoParaManual() {
        if (canalCobro != CanalCobroEnum.MANUAL) {
            return true;
        }
        return medioPago != null && medioPago != MedioPagoEnum.MERCADO_PAGO;
    }
}
