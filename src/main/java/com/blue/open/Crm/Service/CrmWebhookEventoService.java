package com.blue.open.Crm.Service;

import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookAckDataDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookDecisionDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookEventoDTO;
import com.blue.open.Crm.Entity.Db.CrmWebhookEvento;
import com.blue.open.Crm.Repository.CrmWebhookEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resuelve la idempotencia de los webhooks del CRM (blue-crm).
 * <p>
 * Lleva el registro de cada evento en la tabla crm_webhook_eventos y decide, ante cada entrega,
 * si hay que procesarlo, responder el resultado ya conocido, o no hacer nada.
 * <p>
 * Es un bean aparte y no metodos privados de AbstractCrmWebhookProcesador porque cada operacion
 * necesita su propia transaccion: el registro del evento tiene que quedar commiteado antes de
 * que arranque el procesamiento, que puede ser largo. Una llamada interna no pasaria por el
 * proxy de Spring y @Transactional no tendria efecto.
 * <p>
 * Depende de: CrmWebhookEventoRepository. No llama a ningun servicio externo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CrmWebhookEventoService {

    // Minutos que se toleran a un evento en PENDIENTE antes de darlo por colgado y reprocesarlo.
    // El primer reintento del CRM llega a los 5 minutos, asi que este margen evita pisar un
    // procesamiento que todavia esta corriendo
    private static final int MINUTOS_TOLERANCIA_PENDIENTE = 10;

    private final CrmWebhookEventoRepository crmWebhookEventoRepository;

    /**
     * Decide que hacer con un evento recien recibido, dejandolo registrado si es la primera vez.
     * <p>
     * Reglas:
     * - No registrado: se guarda en PENDIENTE y se procesa.
     * - PROCESADO: no se ejecuta nada, se responde el ACK con el resultado guardado.
     * - ERROR: se vuelve a PENDIENTE y se reprocesa, aprovechando el reintento del CRM para
     * recuperarse de fallas transitorias.
     * - PENDIENTE vencido: se asume colgado y se reprocesa.
     * - PENDIENTE reciente: se omite, porque hay otro hilo procesandolo.
     * <p>
     * Espera: el evento parseado y el cuerpo crudo que llego.
     * Responde: la decision a ejecutar por el procesador.
     */
    @Transactional
    public CrmWebhookDecisionDTO evaluar(CrmWebhookEventoDTO evento, String body) {
        Optional<CrmWebhookEvento> registrado = this.crmWebhookEventoRepository
                .findFirstByEventId(evento.getEventId());

        if (registrado.isEmpty()) {
            return this.registrarNuevo(evento, body);
        }

        CrmWebhookEvento registro = registrado.get();

        if (registro.estaProcesado()) {
            log.info("CrmWebhookEventoService | evaluar | evento ya procesado, se responde el ACK guardado "
                    + "| eventId={}", registro.getEventId());
            return CrmWebhookDecisionDTO.responderAck(this.construirAckDeRegistro(registro));
        }

        if (registro.estaEnError()) {
            log.info("CrmWebhookEventoService | evaluar | evento en error, se reprocesa | eventId={}",
                    registro.getEventId());
            return this.reprocesar(registro);
        }

        if (registro.pendienteVencido(MINUTOS_TOLERANCIA_PENDIENTE)) {
            log.warn("CrmWebhookEventoService | evaluar | evento pendiente hace mas de {} minutos, "
                    + "se asume colgado y se reprocesa | eventId={}",
                    MINUTOS_TOLERANCIA_PENDIENTE, registro.getEventId());
            return this.reprocesar(registro);
        }

        log.info("CrmWebhookEventoService | evaluar | evento en proceso por otro hilo, se omite | eventId={}",
                registro.getEventId());
        return CrmWebhookDecisionDTO.omitir();
    }

    /**
     * Deja el evento como procesado con exito. A partir de aca no se vuelve a ejecutar.
     * <p>
     * Espera: el identificador del evento en el CRM y el numero de factura si aplica.
     */
    @Transactional
    public void marcarProcesado(String eventId, String numeroFactura) {
        this.crmWebhookEventoRepository.findFirstByEventId(eventId).ifPresent(registro -> {
            registro.marcarProcesado(numeroFactura);
            this.crmWebhookEventoRepository.save(registro);
        });
    }

    /**
     * Deja el evento como fallido guardando la excepcion completa.
     * <p>
     * Espera: el identificador del evento en el CRM y la excepcion que corto el procesamiento.
     */
    @Transactional
    public void marcarError(String eventId, Throwable excepcion) {
        this.crmWebhookEventoRepository.findFirstByEventId(eventId).ifPresent(registro -> {
            registro.marcarError(excepcion);
            this.crmWebhookEventoRepository.save(registro);
        });
    }

    /**
     * Deja el evento como fallido guardando un motivo en texto, para cuando el procesamiento no
     * lanzo excepcion pero igual termino con error.
     * <p>
     * Espera: el identificador del evento en el CRM y la descripcion del fallo.
     */
    @Transactional
    public void marcarError(String eventId, String detalle) {
        this.crmWebhookEventoRepository.findFirstByEventId(eventId).ifPresent(registro -> {
            registro.marcarError(detalle);
            this.crmWebhookEventoRepository.save(registro);
        });
    }

    // ! ---------------------------------------------------------------
    // ! Infraestructura interna
    // ! ---------------------------------------------------------------

    /**
     * Registra un evento que se ve por primera vez.
     * <p>
     * El indice unico sobre event_id es la ultima linea de defensa ante dos entregas
     * simultaneas: si la insercion choca, significa que otro hilo se adelanto, asi que se omite
     * y se deja que el CRM reintente.
     */
    private CrmWebhookDecisionDTO registrarNuevo(CrmWebhookEventoDTO evento, String body) {
        try {
            this.crmWebhookEventoRepository.save(
                    CrmWebhookEvento.crear(evento.getEventId(), evento.getTipo(), body));
            return CrmWebhookDecisionDTO.procesar();
        } catch (DataIntegrityViolationException e) {
            log.warn("CrmWebhookEventoService | registrarNuevo | el evento ya fue registrado por otro hilo, "
                    + "se omite | eventId={}", evento.getEventId());
            return CrmWebhookDecisionDTO.omitir();
        }
    }

    /** Devuelve el evento a PENDIENTE y lo persiste, refrescando su fecha de actualizacion. */
    private CrmWebhookDecisionDTO reprocesar(CrmWebhookEvento registro) {
        registro.marcarParaReproceso();
        this.crmWebhookEventoRepository.save(registro);
        return CrmWebhookDecisionDTO.procesar();
    }

    /**
     * Arma el ACK de exito a partir de lo que quedo guardado del procesamiento original.
     * El tipo lo completa el procesador con el que llego en el webhook.
     * <p>
     * Solo los PAGO_IMPUTADO tienen numero de factura guardado, que es justamente el dato que
     * el CRM exige para confirmarlos.
     */
    private CrmWebhookAckDataDTO construirAckDeRegistro(CrmWebhookEvento registro) {
        if (registro.getNumeroFactura() == null || registro.getNumeroFactura().isBlank()) {
            return CrmWebhookAckDataDTO.confirmar(registro.getEventId());
        }
        return CrmWebhookAckDataDTO.confirmarPagoImputado(
                registro.getEventId(), registro.getNumeroFactura());
    }
}
