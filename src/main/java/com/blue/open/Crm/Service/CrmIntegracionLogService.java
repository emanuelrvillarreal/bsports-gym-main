package com.blue.open.Crm.Service;

import com.blue.open.Crm.Entity.DTO.CrmIntegracionLogRegistro;
import com.blue.open.Crm.Entity.Db.CrmIntegracionLog;
import com.blue.open.Crm.Repository.CrmIntegracionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda en base cada interaccion con el CRM (blue-crm): webhooks recibidos, ACKs enviados con
 * su respuesta, y llamadas salientes del cliente.
 * <p>
 * Depende de: CrmIntegracionLogRepository. No llama a ningun servicio externo.
 * <p>
 * Es una bitacora de auditoria, no parte del flujo: un fallo al escribir el log nunca puede
 * tumbar el procesamiento de un webhook ni una llamada al CRM.
 */
@Service
@Slf4j
public class CrmIntegracionLogService {

    private final CrmIntegracionLogRepository crmIntegracionLogRepository;

    private final int maxPayloadChars;

    public CrmIntegracionLogService(
            CrmIntegracionLogRepository crmIntegracionLogRepository,
            @Value("${crm.integracion.logs.max-payload-chars:100000}") int maxPayloadChars) {
        this.crmIntegracionLogRepository = crmIntegracionLogRepository;
        this.maxPayloadChars = maxPayloadChars;
    }

    /**
     * Registra una interaccion con el CRM.
     * <p>
     * Corre en una transaccion propia (REQUIRES_NEW) para que la evidencia quede guardada aunque
     * la transaccion que la rodea termine haciendo rollback, que es justo cuando mas se la
     * necesita.
     * <p>
     * Espera: los datos de la interaccion, armados con el builder de CrmIntegracionLogRegistro.
     * Responde: nada. Si la escritura falla solo se loguea, para no afectar el flujo que la
     * origino.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(CrmIntegracionLogRegistro registro) {
        if (registro == null) {
            return;
        }

        try {
            CrmIntegracionLog entrada = new CrmIntegracionLog();
            entrada.setDireccion(registro.getDireccion());
            entrada.setCanal(registro.getCanal());
            entrada.setOperacion(this.truncar(registro.getOperacion(), 120));
            entrada.setTipoEvento(this.truncar(registro.getTipoEvento(), 40));
            entrada.setEventId(this.truncar(registro.getEventId(), 64));
            entrada.setMetodoHttp(this.truncar(registro.getMetodoHttp(), 10));
            entrada.setUrl(this.truncar(registro.getUrl(), 500));
            entrada.setStatusCode(registro.getStatusCode());
            entrada.setExitoso(registro.isExitoso());
            entrada.setRequestPayload(this.truncar(registro.getRequestPayload(), this.maxPayloadChars));
            entrada.setResponsePayload(this.truncar(registro.getResponsePayload(), this.maxPayloadChars));
            entrada.setErrorPayload(this.truncar(registro.getErrorPayload(), this.maxPayloadChars));
            entrada.setDuracionMs(registro.getDuracionMs());

            this.crmIntegracionLogRepository.save(entrada);
        } catch (Exception e) {
            log.warn("CrmIntegracionLogService | registrar | no se pudo guardar el log | canal={} "
                    + "| operacion={} | eventId={} | error: {}",
                    registro.getCanal(), registro.getOperacion(), registro.getEventId(), e.getMessage());
        }
    }

    /** Recorta un texto al largo maximo de su columna, para que un payload grande no rompa el insert. */
    private String truncar(String valor, int max) {
        if (valor == null || valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max);
    }
}
