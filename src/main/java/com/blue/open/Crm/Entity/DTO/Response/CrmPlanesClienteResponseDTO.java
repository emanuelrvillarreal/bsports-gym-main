package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Planes a los que esta suscripto un cliente del CRM.
 * Responde GET integraciones/cliente/planes/{cuit}.
 * <p>
 * El CRM devuelve unicamente las suscripciones cuyos planes pertenecen al sistema externo
 * del token utilizado.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPlanesClienteResponseDTO {

    private String cuit;

    private List<CrmSuscripcionActivaDTO> planes = new ArrayList<>();
}
