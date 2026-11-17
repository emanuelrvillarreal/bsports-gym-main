package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Empresa externa registrada en el CRM.
 * Es el elemento de la lista que responde GET integraciones/obtener-empresas-externas.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmEmpresaExternaResponseDTO {

    private String uuid;

    private String nombre;
}
