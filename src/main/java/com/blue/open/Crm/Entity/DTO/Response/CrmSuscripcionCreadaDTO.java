package com.blue.open.Crm.Entity.DTO.Response;

import com.blue.open.Crm.Util.CrmEpochMillisDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Suscripcion generada por el CRM al suscribir un cliente a un plan.
 * Se anida dentro de CrmSuscripcionResponseDTO (POST integraciones/cliente/suscribir).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmSuscripcionCreadaDTO {

    private String uuidSuscripcion;

    // Factura generada junto con la suscripcion
    private String uuidFacturaInicial;

    private String codigoPlan;

    private String nombrePlan;

    // Se deja como String a proposito. Valores conocidos: BORRADOR, ACTIVA, ANULADA,
    // PENDIENTE_PAGO
    private String estado;

    @JsonDeserialize(using = CrmEpochMillisDeserializer.class)
    private LocalDateTime fechaProximaFacturacion;
}
