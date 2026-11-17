package com.blue.open.Crm.Entity.Enums;

/**
 * Que hacer con un webhook del CRM (blue-crm) recien recibido, segun lo que se sepa de el en la
 * tabla crm_webhook_eventos.
 * Lo resuelve CrmWebhookEventoService y lo ejecuta AbstractCrmWebhookProcesador.
 */
public enum CrmWebhookAccionEnum {

    // Es la primera vez que se ve, o quedo en un estado que amerita volver a ejecutarlo
    PROCESAR,

    // Ya fue procesado con exito: se le responde al CRM el ACK guardado sin ejecutar la logica
    RESPONDER_ACK,

    // Hay otro hilo procesandolo. No se hace nada ni se confirma: el CRM va a reintentar
    OMITIR
}
