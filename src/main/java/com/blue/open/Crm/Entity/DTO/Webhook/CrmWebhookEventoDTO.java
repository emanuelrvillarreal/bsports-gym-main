package com.blue.open.Crm.Entity.DTO.Webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Sobre comun de todos los webhooks que envia el CRM (blue-crm).
 * Lo recibe CrmWebhookController y lo parsea AbstractCrmWebhookProcesador.
 * <p>
 * El CRM usa siempre la misma estructura, cambiando solo el contenido de data segun el tipo:
 * PAGO_IMPUTADO, DEUDA_GENERADA, PLAN_CAMBIO y PRODUCTO_CAMBIO.
 * <p>
 * tipo se modela como String y no como CrmTipoEventoEnum a proposito: si el CRM agrega un tipo
 * nuevo, el sobre igual se parsea y se le puede devolver el mismo valor en el ACK.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmWebhookEventoDTO {

    // Identificador del evento en el CRM. Hay que devolverlo tal cual en el ACK
    private String eventId;

    private String tipo;

    // Contenido del evento, sin interpretar. El procesador lo mapea al DTO que corresponda
    private JsonNode data;
}
