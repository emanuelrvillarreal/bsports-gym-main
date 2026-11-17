package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado monetario de un cliente en el CRM.
 * Responde GET integraciones/obtener-estado-monetario/{cuit}.
 * <p>
 * Atencion: del lado del CRM este endpoint consulta un servicio BPoint marcado como
 * deprecado y solo completa el campo deuda. La lista planes siempre llega vacia.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmEstadoMonetarioResponseDTO {

    // Siempre vacia en la implementacion actual del CRM
    private List<CrmPlanEstadoMonetarioDTO> planes = new ArrayList<>();

    // Deuda total del cliente en su cuenta corriente
    private BigDecimal deuda;
}
