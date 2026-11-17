package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Preferencia de Mercado Pago creada por el CRM al pagar una o mas facturas.
 * Responde POST integraciones/pagos/crear.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoPagoPreferenceResponseDTO {

    private String id;

    @JsonProperty("init_point")
    private String initPoint;
}
