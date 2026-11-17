package com.blue.open.Crm.Controller;

import com.blue.open.Crm.Entity.DTO.CrmIntegracionLogRegistro;
import com.blue.open.Crm.Entity.Enums.CrmLogCanalEnum;
import com.blue.open.Crm.Entity.Enums.CrmLogDireccionEnum;
import com.blue.open.Crm.Service.CrmIntegracionLogService;
import com.blue.open.Crm.Service.CrmWebhookAuthService;
import com.blue.open.Crm.Service.CrmWebhookProcesador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receptor de los webhooks que el CRM (blue-crm) envia a este sistema.
 * <p>
 * Url completa: POST {host}/api/webhookCrm. Es la que hay que cargar en el campo webhook_url
 * del sistema externo de bsports-club dentro del CRM. El CRM exige que sea https.
 * <p>
 * El CRM envia un cuerpo con la forma {"eventId": "...", "tipo": "...", "data": {...}}, donde
 * tipo puede ser PAGO_IMPUTADO, DEUDA_GENERADA, PLAN_CAMBIO o PRODUCTO_CAMBIO, y agrega el
 * header Authorization: Bearer con el token del sistema externo.
 * <p>
 * El controller valida el token, deja constancia de la entrega en crm_integracion_logs y
 * delega: el parseo, el procesamiento de cada tipo de evento y la confirmacion (ACK) al CRM
 * ocurren de forma asincronica en AbstractCrmWebhookProcesador. La logica de negocio de cada
 * evento se implementa sobrescribiendo los metodos de CrmWebhookProcesador.
 */
@RestController
@RequestMapping("webhookCrm")
@Slf4j
public class CrmWebhookController {

    private final CrmWebhookAuthService crmWebhookAuthService;

    private final CrmWebhookProcesador crmWebhookProcesador;

    private final CrmIntegracionLogService crmIntegracionLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrmWebhookController(
            CrmWebhookAuthService crmWebhookAuthService,
            CrmWebhookProcesador crmWebhookProcesador,
            CrmIntegracionLogService crmIntegracionLogService) {
        this.crmWebhookAuthService = crmWebhookAuthService;
        this.crmWebhookProcesador = crmWebhookProcesador;
        this.crmIntegracionLogService = crmIntegracionLogService;
    }

    /**
     * Recibe un webhook del CRM, lo deja registrado y lo deriva al procesador.
     * <p>
     * El cuerpo se toma como texto plano, sin mapear a un DTO, para dejar constancia exacta de
     * lo que envia el CRM aunque el formato no sea el esperado. El parseo lo hace el procesador.
     * <p>
     * Cada entrega deja una fila en crm_integracion_logs, incluidas las rechazadas por token
     * invalido: un 401 repetido es justamente lo que hay que poder ver.
     * <p>
     * Depende de: CrmWebhookAuthService para validar el token del CRM, CrmIntegracionLogService
     * para la auditoria, y CrmWebhookProcesador para el procesamiento, que corre en otro hilo.
     * Espera: el header Authorization con el token del sistema externo y el cuerpo del evento.
     * Responde: 200 sin contenido si el token es valido, 401 si no lo es. Responde apenas
     * encola el procesamiento, sin esperar a que termine.
     */
    @PostMapping
    @Operation(summary = "Recibe los webhooks del CRM", description = "Valida el token del sistema externo y encola el evento para procesarlo de forma asincronica. Tipos soportados: PAGO_IMPUTADO, DEUDA_GENERADA, PLAN_CAMBIO y PRODUCTO_CAMBIO.")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body,
            HttpServletRequest httpRequest) {

        long inicio = System.currentTimeMillis();

        // Se loguea antes de validar para dejar rastro incluso de las llamadas rechazadas
        log.info("CrmWebhookController | recibirWebhook | body={}", body);

        try {
            this.crmWebhookAuthService.validarAutorizacion(authorization);
        } catch (Exception e) {
            this.registrarEntrega(httpRequest, body, HttpStatus.UNAUTHORIZED.value(), false,
                    e.getMessage(), System.currentTimeMillis() - inicio);
            throw e;
        }

        this.registrarEntrega(httpRequest, body, HttpStatus.OK.value(), true, null,
                System.currentTimeMillis() - inicio);

        this.crmWebhookProcesador.procesarAsync(body);

        return ResponseEntity.ok().build();
    }

    /**
     * Deja en crm_integracion_logs la constancia de una entrega recibida del CRM.
     * <p>
     * Espera: el request HTTP, el cuerpo crudo, el status con el que se resuelve la entrega, si
     * fue aceptada, el detalle del rechazo si lo hubo, y cuanto tardo.
     * Responde: nada.
     */
    private void registrarEntrega(
            HttpServletRequest httpRequest,
            String body,
            int statusCode,
            boolean exitoso,
            String errorPayload,
            long duracionMs) {
        this.crmIntegracionLogService.registrar(CrmIntegracionLogRegistro.builder()
                .direccion(CrmLogDireccionEnum.ENTRADA)
                .canal(CrmLogCanalEnum.WEBHOOK)
                .operacion("webhookCrm")
                .eventId(this.leerCampo(body, "eventId"))
                .tipoEvento(this.leerCampo(body, "tipo"))
                .metodoHttp(httpRequest != null ? httpRequest.getMethod() : null)
                .url(httpRequest != null ? httpRequest.getRequestURI() : null)
                .statusCode(statusCode)
                .exitoso(exitoso)
                .requestPayload(body)
                .errorPayload(errorPayload)
                .duracionMs(duracionMs)
                .build());
    }

    /**
     * Extrae un campo de primer nivel del cuerpo recibido, para poder correlacionar la entrega
     * con el evento. Responde null si el cuerpo esta vacio, no es JSON o no trae ese campo.
     */
    private String leerCampo(String body, String campo) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode json = this.objectMapper.readTree(body);
            return json.hasNonNull(campo) ? json.get(campo).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
