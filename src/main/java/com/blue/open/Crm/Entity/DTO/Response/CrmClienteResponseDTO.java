package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Resultado del alta de un cliente en el CRM.
 * Responde POST integraciones/cliente/crear.
 * <p>
 * Si el CUIT ya existia el CRM no falla: responde estado CLIENTE_EXISTENTE, lo que habilita
 * a continuar con la asociacion de planes.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmClienteResponseDTO {

    private String uuidCliente;

    private String cuit;

    // Se deja como String a proposito. Valores conocidos: CLIENTE_CREADO, CLIENTE_EXISTENTE
    private String estado;

    // Mensaje descriptivo del CRM. Al crear el cliente avisa que la cuenta corriente
    // puede demorar unos segundos en estar disponible
    private String mensaje;
}
