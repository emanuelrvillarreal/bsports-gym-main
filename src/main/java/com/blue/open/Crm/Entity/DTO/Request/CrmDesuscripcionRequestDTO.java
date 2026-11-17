package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Pedido de baja de un cliente respecto de uno o mas planes del CRM.
 * Se envia en POST integraciones/cliente/desuscribir.
 * <p>
 * El CRM anula unicamente las suscripciones activas que pertenecen al sistema externo del
 * token usado. Si no encuentra ninguna coincidencia responde 404 "Suscripciones no encontradas".
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmDesuscripcionRequestDTO {

    // CUIT sin guiones, 11 digitos
    private String cuit;

    // Codigos de los planes a dar de baja. Debe tener al menos un elemento no vacio
    private List<String> codigosPlan;
}
