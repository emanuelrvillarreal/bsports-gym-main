package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Provincia del catalogo del CRM, usada para validar codigoProvincia al dar de alta un club.
 * <p>
 * Depende de: GET clientes/provincias/obtener del CRM.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmProvinciaResponseDTO {

    private String uuid;

    private String nombre;

    // Codigo con el que se referencia la provincia al dar de alta un club (codigoProvincia)
    private String codigo;

    private String pais;

    private String paisUuid;
}
