package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CobroCuotaCommand {

    @NotEmpty(message = "Debe indicar al menos una cuota")
    private List<@NotBlank(message = "UUID de cuota inválido") String> cuotaUuids;

    @NotNull(message = "Canal de cobro requerido")
    private CanalCobroEnum canalCobro;

    @NotNull(message = "Medio de pago requerido")
    private MedioPagoEnum medioPago;

    @PastOrPresent(message = "La fecha de cobro no puede ser futura")
    private LocalDate fecha;

    private String observaciones;
}