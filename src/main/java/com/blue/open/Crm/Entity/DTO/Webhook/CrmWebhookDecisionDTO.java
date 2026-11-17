package com.blue.open.Crm.Entity.DTO.Webhook;

import com.blue.open.Crm.Entity.Enums.CrmWebhookAccionEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * Que hacer con un webhook recien recibido del CRM (blue-crm).
 * <p>
 * Lo devuelve CrmWebhookEventoService.evaluar despues de consultar la tabla de eventos, y lo
 * ejecuta AbstractCrmWebhookProcesador. Existe porque hay tres desenlaces posibles y no
 * alcanza con un booleano: procesar el evento, responder directamente un ACK ya conocido, o no
 * hacer nada.
 */
@Getter
@Setter
public class CrmWebhookDecisionDTO {

    private CrmWebhookAccionEnum accion;

    // Solo viene cargado cuando la accion es RESPONDER_ACK
    private CrmWebhookAckDataDTO ackData;

    /** El evento tiene que ejecutarse: es nuevo, fallo antes o quedo colgado. */
    public static CrmWebhookDecisionDTO procesar() {
        CrmWebhookDecisionDTO decision = new CrmWebhookDecisionDTO();
        decision.setAccion(CrmWebhookAccionEnum.PROCESAR);
        return decision;
    }

    /**
     * El evento ya se proceso con exito: hay que confirmarle al CRM el resultado guardado sin
     * volver a ejecutar la logica.
     */
    public static CrmWebhookDecisionDTO responderAck(CrmWebhookAckDataDTO ackData) {
        CrmWebhookDecisionDTO decision = new CrmWebhookDecisionDTO();
        decision.setAccion(CrmWebhookAccionEnum.RESPONDER_ACK);
        decision.setAckData(ackData);
        return decision;
    }

    /**
     * Hay otro hilo procesando el evento. No se ejecuta nada ni se confirma: el CRM va a
     * reintentar mas tarde.
     */
    public static CrmWebhookDecisionDTO omitir() {
        CrmWebhookDecisionDTO decision = new CrmWebhookDecisionDTO();
        decision.setAccion(CrmWebhookAccionEnum.OMITIR);
        return decision;
    }
}
