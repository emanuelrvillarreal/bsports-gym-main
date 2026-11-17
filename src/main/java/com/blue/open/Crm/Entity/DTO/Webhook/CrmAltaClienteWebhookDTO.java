package com.blue.open.Crm.Entity.DTO.Webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Contenido del webhook ALTA_CLIENTE del CRM (blue-crm): un cliente se dio de alta en este
 * sistema, al pagarse la factura inicial de su suscripcion de inscripcion. Es el campo data de
 * CrmWebhookEventoDTO.
 * <p>
 * El CRM manda ademas codigoCondicionImpositiva y estadoRegistro, que no se usan.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmAltaClienteWebhookDTO {

    // Identificador del cliente en el CRM. Es el que llega en el resto de los webhooks
    private String uuidCliente;

    private String razonSocial;

    // CUIT del cliente, 11 digitos sin guiones
    private String cuit;

    // Correo de facturacion del cliente
    private String correoFacturacion;
}
