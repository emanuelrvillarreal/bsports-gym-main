package com.blue.open.Crm.Entity.DTO.Webhook;

import lombok.Getter;
import lombok.Setter;

/**
 * Resultado del procesamiento de un webhook del CRM (blue-crm), con los datos necesarios para
 * confirmarlo.
 * <p>
 * Es plomeria interna del procesador: lo arman AbstractCrmWebhookProcesador, al terminar de
 * ejecutar un evento, y CrmWebhookEventoService, al reconstruir el ACK de un evento ya
 * procesado. Quien implementa un evento no lo necesita ni lo ve, porque los metodos onXxx no
 * devuelven nada.
 * <p>
 * Se construye con las factories estaticas. El tipo lo completa el procesador antes de enviar
 * el ACK.
 *
 * <pre>
 * CrmWebhookAckDataDTO.confirmarPagoImputado(eventId, numeroFactura)  // solo PAGO_IMPUTADO
 * CrmWebhookAckDataDTO.confirmar(eventId)                             // resto de los tipos
 * CrmWebhookAckDataDTO.error(eventId, "motivo")                       // el CRM reintenta
 * </pre>
 *
 * Hay dos formas de confirmar y no una sola con el numero de factura opcional porque
 * PAGO_IMPUTADO es el unico evento al que el CRM le exige ese dato: confirmarlo sin el hace que
 * el CRM responda 400 y el evento nunca quede cerrado.
 */
@Getter
@Setter
public class CrmWebhookAckDataDTO {

    // Identificador del evento en el CRM, tal cual llego en el webhook
    private String eventId;

    // Tipo del evento, tal cual lo mando el CRM. Lo completa el procesador
    private String tipo;

    // true marca el evento como FINALIZADO en el CRM, false lo marca como ERROR y lo reintenta
    private boolean ok;

    // Obligatorio para confirmar un PAGO_IMPUTADO con ok en true
    private String numeroFactura;

    // Descripcion del error. El CRM la guarda cuando ok es false
    private String detalle;

    /**
     * Confirma un evento que no necesita datos adicionales: DEUDA_GENERADA, PLAN_CAMBIO y
     * PRODUCTO_CAMBIO.
     * <p>
     * No usar en PAGO_IMPUTADO: para ese caso existe confirmarPagoImputado, porque el CRM
     * rechaza el ACK si no viaja el numero de factura.
     * <p>
     * Espera: el eventId que recibe como parametro el metodo del evento.
     */
    public static CrmWebhookAckDataDTO confirmar(String eventId) {
        CrmWebhookAckDataDTO ackData = new CrmWebhookAckDataDTO();
        ackData.setEventId(eventId);
        ackData.setOk(true);
        return ackData;
    }

    /**
     * Confirma un evento PAGO_IMPUTADO, el unico que exige informar el numero de factura.
     * <p>
     * El numero tiene que ser el que llego en el webhook (data.getNumeroFactura()): el CRM
     * valida que coincida con el que guardo y, si no, responde 400 y el evento queda sin
     * cerrar.
     * <p>
     * Espera: el eventId que recibe como parametro el metodo del evento, y el numero de
     * factura que llego en el webhook.
     */
    public static CrmWebhookAckDataDTO confirmarPagoImputado(String eventId, String numeroFactura) {
        CrmWebhookAckDataDTO ackData = confirmar(eventId);
        ackData.setNumeroFactura(numeroFactura);
        return ackData;
    }

    /**
     * Marca el evento como fallido. El CRM guarda el detalle y reintenta el envio con backoff
     * (5, 15, 60, 360 y 1440 minutos, hasta 10 intentos).
     * <p>
     * Espera: el eventId que recibe como parametro el metodo del evento, y el motivo del error.
     */
    public static CrmWebhookAckDataDTO error(String eventId, String detalle) {
        CrmWebhookAckDataDTO ackData = new CrmWebhookAckDataDTO();
        ackData.setEventId(eventId);
        ackData.setOk(false);
        ackData.setDetalle(detalle);
        return ackData;
    }
}
