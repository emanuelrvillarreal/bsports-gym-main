package com.blue.open.Crm.Entity.DTO.Response;

import com.blue.open.Crm.Util.CrmEpochMillisDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Suscripcion vigente de un cliente a un plan del CRM.
 * Se anida dentro de CrmPlanesClienteResponseDTO (GET integraciones/cliente/planes/{cuit}).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmSuscripcionActivaDTO {

    private String uuidSuscripcion;

    private String codigoPlan;

    private String nombrePlan;

    @JsonDeserialize(using = CrmEpochMillisDeserializer.class)
    private LocalDateTime fechaInicio;

    @JsonDeserialize(using = CrmEpochMillisDeserializer.class)
    private LocalDateTime fechaProximaFacturacion;

    // Se deja como String a proposito: valores conocidos BORRADOR, ACTIVA, ANULADA y
    // PENDIENTE_PAGO, pero un enum romperia la lectura si el CRM agrega un estado nuevo
    private String estadoSuscripcion;
}
