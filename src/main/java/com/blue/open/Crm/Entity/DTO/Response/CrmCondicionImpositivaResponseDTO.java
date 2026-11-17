package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Condicion impositiva del catalogo del CRM, usada para validar codigoCondicionImpositiva al
 * dar de alta un club.
 * <p>
 * Depende de: GET clientes/condicion-impositiva/obtener del CRM.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmCondicionImpositivaResponseDTO {

    private String uuid;

    private String nombre;

    // Codigo con el que se referencia la condicion impositiva al dar de alta un club
    // (codigoCondicionImpositiva)
    private String codigo;
}
