package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Resultado de dar de baja las suscripciones de un cliente a determinados planes.
 * Responde POST integraciones/cliente/desuscribir.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmDesuscripcionResponseDTO {

    private String cuit;

    // Mensaje descriptivo del CRM, ej: "Suscripciones anuladas correctamente."
    private String mensaje;
}
