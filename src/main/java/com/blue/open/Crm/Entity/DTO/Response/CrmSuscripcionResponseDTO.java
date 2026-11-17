package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de suscribir un cliente a uno o mas planes del CRM.
 * Responde POST integraciones/cliente/suscribir.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmSuscripcionResponseDTO {

    private String uuidCuentaCorriente;

    private String cuit;

    // Se deja como String a proposito. Valor conocido: SUSCRIPCIONES_CREADAS
    private String estado;

    private String mensaje;

    // Una entrada por cada plan suscripto, con su factura inicial
    private List<CrmSuscripcionCreadaDTO> suscripciones = new ArrayList<>();
}
