package com.blue.open.Crm.Entity.DTO.Request;

import com.blue.open.Crm.Util.CrmEpochMillisSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido de suscripcion de un cliente a uno o mas planes del CRM.
 * Se envia en POST integraciones/cliente/suscribir.
 * <p>
 * El CRM normaliza los codigos de plan (trim y distinct) y crea una suscripcion por cada uno,
 * junto con la factura inicial correspondiente.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmSuscripcionRequestDTO {

    // CUIT sin guiones, 11 digitos
    private String cuit;

    // Codigos de los planes a suscribir. Debe tener al menos un elemento no vacio
    private List<String> codigosPlan;

    // Opcional. Si viaja null, el CRM toma la fecha y hora actual
    @JsonSerialize(using = CrmEpochMillisSerializer.class)
    private LocalDateTime fechaInicio;

    // Opcional. Si viaja null, las suscripciones se crean sin descuento
    private CrmDescuentoRequestDTO descuento;
}
