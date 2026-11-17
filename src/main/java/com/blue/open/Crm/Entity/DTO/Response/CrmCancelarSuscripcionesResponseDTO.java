package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Resultado de cancelar todas las suscripciones de un cliente en el CRM.
 * Responde POST integraciones/cliente/cancelar-suscripciones/{cuit}.
 * <p>
 * Ademas de anular las suscripciones, el CRM compensa las facturas pendientes del cliente.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmCancelarSuscripcionesResponseDTO {

    private String cuit;

    // Cantidad de suscripciones que quedaron anuladas
    private Integer suscripcionesAnuladas;

    // Cantidad de facturas pendientes que el CRM compenso
    private Integer facturasCompensadas;

    // Monto total compensado en esas facturas
    private BigDecimal montoCompensado;

    private String mensaje;
}
