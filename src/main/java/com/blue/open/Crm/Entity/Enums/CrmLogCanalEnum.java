package com.blue.open.Crm.Entity.Enums;

/**
 * Tipo de interaccion con el CRM (blue-crm) registrada en crm_integracion_logs.
 * Separa los ACKs del resto de las llamadas salientes porque son las dos puntas del circuito de
 * webhooks y conviene poder filtrarlas por separado.
 */
public enum CrmLogCanalEnum {

    // Webhook que el CRM nos envio
    WEBHOOK,

    // Confirmacion que le enviamos al CRM por un webhook recibido
    ACK,

    // Cualquier otra llamada del cliente al CRM: planes, productos, clientes, facturas
    API
}
