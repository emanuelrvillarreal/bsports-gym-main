package com.blue.open.Crm.Entity.Enums;

/**
 * Estado de procesamiento de un webhook recibido del CRM (blue-crm).
 * Lo usa la entidad CrmWebhookEvento para su maquina de estados.
 * <p>
 * PROCESADO es el unico estado terminal: un evento en ERROR se vuelve a ejecutar cuando el CRM
 * reintenta, para poder recuperarse de fallas transitorias.
 */
public enum CrmWebhookEstadoEnum {

    // Recibido y todavia en proceso. Si queda mucho tiempo asi, se asume colgado y se reprocesa
    PENDIENTE,

    // Procesado con exito. No se vuelve a ejecutar: se le responde al CRM el ACK guardado
    PROCESADO,

    // Fallo el procesamiento. Se reprocesa en el proximo reintento del CRM
    ERROR
}
