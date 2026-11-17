package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CobroCuotaResultDTO {

    private List<CobroCuotaItemResultDTO> items = new ArrayList<>();
    private int exitosas;
    private int fallidas;

    private String mercadoPagoPreferenceId;
    private String mercadoPagoInitPoint;
    private String mercadoPagoSandboxInitPoint;
}
