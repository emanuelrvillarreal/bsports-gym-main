package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Confirmacion (ACK) del procesamiento de un webhook recibido desde el CRM.
 * Se envia en POST integraciones/webhooks/pagos/ack.
 * <p>
 * Sin este ACK el CRM reintenta el envio del evento con backoff (5, 15, 60, 360 y 1440
 * minutos, hasta 10 intentos).
 * <p>
 * Contenido esperado en data segun el caso:
 * - tipo PAGO_IMPUTADO: es obligatorio incluir "numeroFactura" y debe coincidir con el que
 * el CRM guardo para ese evento, si no responde 400 "Numero de factura invalido". El resto de
 * los tipos se confirman por la via generica, que no lo pide.
 * - ok en false: el CRM guarda data.detalle como descripcion del error.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmWebhookAckRequestDTO {

    // Identificador del evento, tal cual llego en el webhook del CRM
    private String eventId;

    // Tipo del evento, tal cual llego en el webhook. Es String y no CrmTipoEventoEnum porque
    // el CRM busca el evento por eventId + tipo: hay que devolverle el mismo valor que mando,
    // incluso si es un tipo que todavia no modelamos
    private String tipo;

    // true marca el evento como FINALIZADO en el CRM, false lo marca como ERROR
    private Boolean ok;

    // Datos libres del ACK. Ver el comentario de la clase para el contenido obligatorio
    private Map<String, Object> data;
}
