package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Periodicidad de facturacion de un plan del CRM.
 * Se anida dentro de CrmPlanResponseDTO (GET integraciones/obtener-planes).
 * Puede llegar null si el plan no tiene periodicidad configurada.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPeriodicidadDTO {

    // Codigo de periodicidad segun el catalogo del CRM, ej: mensual, anual
    private String codigo;
}
