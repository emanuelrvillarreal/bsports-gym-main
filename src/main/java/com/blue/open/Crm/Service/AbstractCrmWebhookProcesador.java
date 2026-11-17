package com.blue.open.Crm.Service;

import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.DTO.Request.CrmWebhookAckRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProductoResponseDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmAltaClienteWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmDeudaGeneradaWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmPagoImputadoWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookAckDataDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookDecisionDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmWebhookEventoDTO;
import com.blue.open.Crm.Entity.Enums.CrmTipoEventoEnum;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Procesa los webhooks que envia el CRM (blue-crm): parsea el evento, controla que no se haya
 * procesado antes, lo deriva al metodo que corresponde segun el tipo y confirma el resultado.
 * <p>
 * Para implementar un evento hay que sobrescribir su metodo en la subclase concreta
 * (CrmWebhookProcesador). Los metodos que no se sobrescriben solo dejan un log y responden un
 * ACK con error, de modo que el CRM reintente y ningun evento se pierda en silencio.
 * <p>
 * Depende de: CrmWebhookEventoService para la idempotencia, y CrmIntegracionService para enviar
 * el ACK, que a su vez depende del servicio externo blue-crm.
 * Espera: el cuerpo crudo del webhook, tal cual lo recibe CrmWebhookController.
 * Responde: nada. El resultado se informa al CRM por el ACK y queda en el log.
 * <p>
 * Las dependencias se inyectan por campo y no por constructor a proposito: asi la subclase
 * concreta no necesita declarar constructor, y agregarle una dependencia nueva a esta clase no
 * obliga a tocarla.
 */
@Slf4j
public abstract class AbstractCrmWebhookProcesador {

    /**
     * Esperas (en segundos) antes de reintentar el ACK cuando el CRM responde que no conoce el evento.
     * <p>
     * El CRM a veces manda el webhook antes de terminar de guardar su transaccion: si este sistema confirma
     * muy rapido, el CRM todavia no ve el evento y responde 404. Por eso se reintenta un par de veces. Si
     * despues del ultimo intento el CRM sigue sin conocerlo, es que deshizo la operacion (por ejemplo, fallo
     * la creacion del pago en Mercado Pago) y lo que se registro aca hay que descartarlo.
     */
    private static final long[] ESPERAS_REINTENTO_ACK_SEG = {5, 20, 60};

    private enum ResultadoAck { OK, EVENTO_INEXISTENTE, ERROR }

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private CrmWebhookEventoService crmWebhookEventoService;

    // Mapper propio: el CRM puede agregar campos nuevos a los payloads sin avisar
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Procesa el webhook fuera del hilo del request, para que el controller pueda responderle
     * 200 al CRM enseguida y este no quede esperando mientras se procesa y se envia el ACK.
     * <p>
     * Usa AsyncTaskExecutor, el pool que AsyncConfig destina a webhooks y eventos.
     */
    @Async("AsyncTaskExecutor")
    public void procesarAsync(String body) {
        this.procesar(body);
    }

    /**
     * Parsea el evento, controla la idempotencia, lo deriva al metodo del tipo correspondiente
     * y envia el ACK con lo que ese metodo haya devuelto.
     * <p>
     * Si el evento ya fue procesado con exito no se ejecuta nada: se le responde al CRM el ACK
     * guardado. Ver CrmWebhookEventoService para el resto de las reglas.
     * <p>
     * Es final para que el orden parseo, idempotencia, procesamiento y ACK no se pueda alterar
     * desde la subclase: lo unico que se sobrescribe son los metodos por tipo de evento.
     * <p>
     * Espera: el cuerpo crudo del webhook.
     * Responde: nada. Nunca propaga excepciones; cualquier falla se convierte en un ACK con
     * error para que el CRM la registre y reintente.
     */
    public final void procesar(String body) {
        CrmWebhookEventoDTO evento;
        try {
            evento = this.parsearEvento(body);
        } catch (Exception e) {
            // Sin eventId no hay forma de confirmarle nada al CRM: solo queda el log
            log.error("AbstractCrmWebhookProcesador | procesar | no se pudo interpretar el webhook | body={}",
                    body, e);
            return;
        }

        CrmWebhookDecisionDTO decision = this.crmWebhookEventoService.evaluar(evento, body);
        switch (decision.getAccion()) {
            case OMITIR -> {
                return;
            }
            case RESPONDER_ACK -> {
                this.enviarAck(this.completarAck(decision.getAckData(), evento));
                return;
            }
            case PROCESAR -> {
                // Sigue con el procesamiento normal
            }
        }

        CrmWebhookAckDataDTO ackData;
        try {
            ackData = this.ejecutarEvento(evento);
            this.crmWebhookEventoService.marcarProcesado(evento.getEventId(), ackData.getNumeroFactura());
        } catch (Exception e) {
            log.error("AbstractCrmWebhookProcesador | procesar | error procesando el evento | eventId={} "
                    + "| tipo={} | error: {}", evento.getEventId(), evento.getTipo(), e.getMessage(), e);
            // Se guarda la excepcion completa como motivo, para poder diagnosticarla despues
            this.crmWebhookEventoService.marcarError(evento.getEventId(), e);
            ackData = CrmWebhookAckDataDTO.error(evento.getEventId(),
                    e.getMessage() != null ? e.getMessage() : "Error al procesar el evento");
        }

        CrmWebhookAckDataDTO ack = this.completarAck(ackData, evento);
        ResultadoAck resultado = this.intentarAck(ack);
        if (resultado == ResultadoAck.EVENTO_INEXISTENTE && ack.isOk()) {
            this.reintentarAck(ack, evento, 0);
        }
    }

    /**
     * Reintenta el ACK de un evento que el CRM dijo no conocer. Si en algun intento lo reconoce, listo.
     * Si despues del ultimo sigue sin conocerlo, se considera que el CRM deshizo la operacion y se le
     * pide a la subclase que revierta lo registrado (onEventoInexistenteEnCrm).
     */
    private void reintentarAck(CrmWebhookAckDataDTO ack, CrmWebhookEventoDTO evento, int intento) {
        long espera = ESPERAS_REINTENTO_ACK_SEG[intento];
        CompletableFuture.runAsync(() -> {
            ResultadoAck resultado = this.intentarAck(ack);
            if (resultado != ResultadoAck.EVENTO_INEXISTENTE) {
                return;
            }
            if (intento + 1 < ESPERAS_REINTENTO_ACK_SEG.length) {
                this.reintentarAck(ack, evento, intento + 1);
                return;
            }
            log.warn("AbstractCrmWebhookProcesador | reintentarAck | el CRM no reconoce el evento despues de "
                    + "reintentar, se descarta | eventId={} | tipo={}", evento.getEventId(), evento.getTipo());
            try {
                this.onEventoInexistenteEnCrm(CrmTipoEventoEnum.desdeTexto(evento.getTipo()), evento.getData(),
                        evento.getEventId());
                this.crmWebhookEventoService.marcarError(evento.getEventId(), new IllegalStateException(
                        "Descartado: el CRM no reconoce el evento (deshizo la operacion)"));
            } catch (Exception e) {
                log.error("AbstractCrmWebhookProcesador | reintentarAck | no se pudo descartar el evento "
                        + "| eventId={} | error: {}", evento.getEventId(), e.getMessage(), e);
            }
        }, CompletableFuture.delayedExecutor(espera, TimeUnit.SECONDS));
    }

    /**
     * Se ejecuta cuando, despues de reintentar el ACK, el CRM sigue sin conocer un evento que este sistema
     * ya proceso: el CRM deshizo la operacion y hay que revertir lo que se registro.
     * <p>
     * Por defecto solo deja un log. La subclase sobrescribe los tipos que dejan datos (p. ej. la factura
     * de DEUDA_GENERADA).
     */
    protected void onEventoInexistenteEnCrm(CrmTipoEventoEnum tipo, JsonNode data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onEventoInexistenteEnCrm | sin reversion para el tipo "
                + "| eventId={} | tipo={}", eventId, tipo);
    }

    // ! ---------------------------------------------------------------
    // ! Metodos a sobrescribir: uno por tipo de evento
    // ! ---------------------------------------------------------------

    /**
     * Se ejecuta cuando el CRM informa que se imputo un pago a una factura del cliente.
     * <p>
     * Espera: los datos del pago y el identificador del evento, este ultimo solo para loguear.
     * Responde: nada. Si el metodo retorna, el evento se da por procesado y esta clase arma el
     * ACK sola, tomando el numero de factura del propio webhook. Si algo falla hay que lanzar
     * una excepcion: el CRM la registra y reintenta el evento.
     */
    protected void onPagoImputado(CrmPagoImputadoWebhookDTO data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onPagoImputado | evento sin implementar | eventId={} "
                + "| numeroFactura={} | cuit={}", eventId, data.getNumeroFactura(), data.getCuit());
        throw new CustomException("Evento PAGO_IMPUTADO no implementado",
                "AbstractCrmWebhookProcesador | onPagoImputado | sin implementar | eventId: " + eventId);
    }

    /**
     * Se ejecuta cuando el CRM informa que se genero deuda para el cliente.
     * <p>
     * Espera: los datos de la deuda y el identificador del evento, este ultimo solo para loguear.
     * Responde: nada. Si el metodo retorna, el evento se da por procesado. Si algo falla hay que
     * lanzar una excepcion: el CRM la registra y reintenta el evento.
     */
    protected void onDeudaGenerada(CrmDeudaGeneradaWebhookDTO data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onDeudaGenerada | evento sin implementar | eventId={} "
                + "| cuit={}", eventId, data.getCuit());
        throw new CustomException("Evento DEUDA_GENERADA no implementado",
                "AbstractCrmWebhookProcesador | onDeudaGenerada | sin implementar | eventId: " + eventId);
    }

    /**
     * Se ejecuta cuando se da de alta o se modifica un plan en el CRM.
     * <p>
     * Espera: el plan con el mismo formato que devuelve obtenerPlanes, y el identificador del
     * evento, este ultimo solo para loguear.
     * Responde: nada. Si el metodo retorna, el evento se da por procesado. Si algo falla hay que
     * lanzar una excepcion: el CRM la registra y reintenta el evento.
     */
    protected void onPlanCambio(CrmPlanResponseDTO data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onPlanCambio | evento sin implementar | eventId={} "
                + "| codigo={}", eventId, data.getCodigo());
        throw new CustomException("Evento PLAN_CAMBIO no implementado",
                "AbstractCrmWebhookProcesador | onPlanCambio | sin implementar | eventId: " + eventId);
    }

    /**
     * Se ejecuta cuando se da de alta o se modifica un producto en el CRM.
     * <p>
     * Espera: el producto con el mismo formato que devuelve obtenerProductos, y el
     * identificador del evento, este ultimo solo para loguear.
     * Responde: nada. Si el metodo retorna, el evento se da por procesado. Si algo falla hay que
     * lanzar una excepcion: el CRM la registra y reintenta el evento.
     */
    protected void onProductoCambio(CrmProductoResponseDTO data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onProductoCambio | evento sin implementar | eventId={} "
                + "| codigo={}", eventId, data.getCodigo());
        throw new CustomException("Evento PRODUCTO_CAMBIO no implementado",
                "AbstractCrmWebhookProcesador | onProductoCambio | sin implementar | eventId: " + eventId);
    }

    /**
     * Se ejecuta cuando el CRM informa que un cliente se dio de alta en este sistema.
     * <p>
     * Espera: los datos del cliente y el identificador del evento, este ultimo solo para loguear.
     * Responde: nada. Si el metodo retorna, el evento se da por procesado. Si algo falla hay que
     * lanzar una excepcion: el CRM la registra y reintenta el evento.
     */
    protected void onAltaCliente(CrmAltaClienteWebhookDTO data, String eventId) {
        log.warn("AbstractCrmWebhookProcesador | onAltaCliente | evento sin implementar | eventId={} "
                + "| uuidCliente={}", eventId, data.getUuidCliente());
        throw new CustomException("Evento ALTA_CLIENTE no implementado",
                "AbstractCrmWebhookProcesador | onAltaCliente | sin implementar | eventId: " + eventId);
    }

    // ! ---------------------------------------------------------------
    // ! Envio del ACK
    // ! ---------------------------------------------------------------

    /**
     * Confirma al CRM el resultado del procesamiento del evento.
     * <p>
     * Depende de: CrmIntegracionService.ackWebhook y, a traves de el, del CRM.
     * Espera: el resultado devuelto por el metodo del evento, ya con eventId y tipo cargados.
     * Responde: nada. Si el ACK falla se loguea pero no se propaga: el CRM va a reintentar el
     * evento igual, y cortar aca dejaria el hilo async con una excepcion sin manejar.
     */
    protected void enviarAck(CrmWebhookAckDataDTO ackData) {
        this.intentarAck(ackData);
    }

    /** Envia el ACK y devuelve si salio bien, si el CRM no conoce el evento (404) o si fallo por otra cosa. */
    private ResultadoAck intentarAck(CrmWebhookAckDataDTO ackData) {
        try {
            CrmWebhookAckRequestDTO request = new CrmWebhookAckRequestDTO();
            request.setEventId(ackData.getEventId());
            request.setTipo(ackData.getTipo());
            request.setOk(ackData.isOk());
            request.setData(this.construirData(ackData));

            this.crmIntegracionService.ackWebhook(request);
            log.info("AbstractCrmWebhookProcesador | enviarAck | eventId={} | tipo={} | ok={}",
                    ackData.getEventId(), ackData.getTipo(), ackData.isOk());
            return ResultadoAck.OK;
        } catch (Exception e) {
            log.error("AbstractCrmWebhookProcesador | enviarAck | no se pudo confirmar el evento | eventId={} "
                    + "| tipo={} | error: {}", ackData.getEventId(), ackData.getTipo(), e.getMessage(), e);
            String detalle = e instanceof CustomException ce ? ce.getDeveloperMessage() : e.getMessage();
            return detalle != null && detalle.contains("status=404") ? ResultadoAck.EVENTO_INEXISTENTE
                    : ResultadoAck.ERROR;
        }
    }

    // ! ---------------------------------------------------------------
    // ! Infraestructura interna
    // ! ---------------------------------------------------------------

    /**
     * Parsea el campo data segun el tipo, invoca el metodo del evento y arma el ACK que
     * corresponde. Es el unico lugar donde se construye el ACK de un evento procesado.
     * <p>
     * Los metodos del evento no devuelven nada: si retornan, el evento se da por procesado. Por
     * eso el numero de factura del ACK de PAGO_IMPUTADO se toma del propio webhook, que es el
     * unico valor que el CRM da por valido.
     * <p>
     * No atrapa las excepciones a proposito: las deja propagar para que procesar pueda guardar
     * la excepcion completa como motivo del error.
     */
    private CrmWebhookAckDataDTO ejecutarEvento(CrmWebhookEventoDTO evento) throws Exception {
        CrmTipoEventoEnum tipo = CrmTipoEventoEnum.desdeTexto(evento.getTipo());
        if (tipo == null) {
            throw new CustomException("Tipo de evento no soportado: " + evento.getTipo(),
                    "AbstractCrmWebhookProcesador | ejecutarEvento | tipo desconocido | eventId: "
                            + evento.getEventId() + " | tipo: " + evento.getTipo());
        }

        String eventId = evento.getEventId();
        switch (tipo) {
            case PAGO_IMPUTADO -> {
                CrmPagoImputadoWebhookDTO data = this.parsearData(evento, CrmPagoImputadoWebhookDTO.class);
                this.onPagoImputado(data, eventId);
                return CrmWebhookAckDataDTO.confirmarPagoImputado(eventId, data.getNumeroFactura());
            }
            case DEUDA_GENERADA -> {
                this.onDeudaGenerada(this.parsearData(evento, CrmDeudaGeneradaWebhookDTO.class), eventId);
                return CrmWebhookAckDataDTO.confirmar(eventId);
            }
            case PLAN_CAMBIO -> {
                this.onPlanCambio(this.parsearData(evento, CrmPlanResponseDTO.class), eventId);
                return CrmWebhookAckDataDTO.confirmar(eventId);
            }
            case PRODUCTO_CAMBIO -> {
                this.onProductoCambio(this.parsearData(evento, CrmProductoResponseDTO.class), eventId);
                return CrmWebhookAckDataDTO.confirmar(eventId);
            }
            case ALTA_CLIENTE -> {
                this.onAltaCliente(this.parsearData(evento, CrmAltaClienteWebhookDTO.class), eventId);
                return CrmWebhookAckDataDTO.confirmar(eventId);
            }
        }

        // Inalcanzable: el switch cubre todos los valores del enum
        throw new CustomException("Tipo de evento no soportado: " + evento.getTipo(),
                "AbstractCrmWebhookProcesador | ejecutarEvento | tipo sin rama | eventId: " + eventId);
    }

    /**
     * Completa el ACK con los datos del sobre antes de enviarlo.
     * <p>
     * El tipo siempre sale del webhook, porque el CRM busca el evento por eventId mas tipo. El
     * eventId lo carga la factory con el que recibe el metodo del evento; se completa aca solo
     * por las dudas, ya que sin el el CRM no puede identificar que confirmar.
     */
    private CrmWebhookAckDataDTO completarAck(CrmWebhookAckDataDTO ackData, CrmWebhookEventoDTO evento) {
        if (ackData.getEventId() == null || ackData.getEventId().isBlank()) {
            ackData.setEventId(evento.getEventId());
        }
        ackData.setTipo(evento.getTipo());
        return ackData;
    }

    /**
     * Parsea el sobre del webhook y valida lo minimo para poder confirmarlo despues.
     * Lanza excepcion si el cuerpo no es interpretable o no trae eventId, porque sin eventId
     * el CRM no puede identificar el evento en el ACK.
     */
    private CrmWebhookEventoDTO parsearEvento(String body) throws Exception {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del webhook esta vacio");
        }

        CrmWebhookEventoDTO evento = this.objectMapper.readValue(body, CrmWebhookEventoDTO.class);
        if (evento == null || evento.getEventId() == null || evento.getEventId().isBlank()) {
            throw new IllegalArgumentException("El webhook no trae eventId");
        }
        return evento;
    }

    /**
     * Convierte el campo data del evento al DTO del tipo indicado.
     * Responde una instancia vacia si data viene nulo, para que el metodo del evento no tenga
     * que contemplar el caso.
     */
    private <T> T parsearData(CrmWebhookEventoDTO evento, Class<T> clase) throws Exception {
        JsonNode data = evento.getData();
        if (data == null || data.isNull()) {
            return clase.getDeclaredConstructor().newInstance();
        }
        return this.objectMapper.treeToValue(data, clase);
    }

    /**
     * Arma el campo data del ACK: numeroFactura cuando el evento se confirma con exito,
     * detalle cuando se confirma con error.
     */
    private Map<String, Object> construirData(CrmWebhookAckDataDTO ackData) {
        Map<String, Object> data = new HashMap<>();
        if (ackData.getNumeroFactura() != null && !ackData.getNumeroFactura().isBlank()) {
            data.put("numeroFactura", ackData.getNumeroFactura());
        }
        if (ackData.getDetalle() != null && !ackData.getDetalle().isBlank()) {
            data.put("detalle", ackData.getDetalle());
        }
        return data;
    }
}
