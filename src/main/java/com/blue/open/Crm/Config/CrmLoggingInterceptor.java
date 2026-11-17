package com.blue.open.Crm.Config;

import com.blue.open.Crm.Entity.DTO.CrmIntegracionLogRegistro;
import com.blue.open.Crm.Entity.Enums.CrmLogCanalEnum;
import com.blue.open.Crm.Entity.Enums.CrmLogDireccionEnum;
import com.blue.open.Crm.Service.CrmIntegracionLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Registra en crm_integracion_logs todas las llamadas salientes al CRM (blue-crm).
 * <p>
 * Se engancha como interceptor del RestClient en CrmRestClientConfig, de modo que cubre los
 * ACKs y las once operaciones de CrmIntegracionService sin que ninguna de ellas tenga que saber
 * que existe, y cubre tambien las que se agreguen en el futuro.
 * <p>
 * Solo observa: cualquier excepcion de la llamada se registra y se relanza tal cual, sin alterar
 * el comportamiento del cliente. Un fallo al loguear tampoco corta la llamada.
 * <p>
 * Requiere que el RestClient use un BufferingClientHttpRequestFactory: sin eso, leer el cuerpo
 * de la respuesta aca consumiria el stream y el RestClient recibiria un body vacio.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CrmLoggingInterceptor implements ClientHttpRequestInterceptor {

    // Endpoint del CRM que confirma un webhook. Se distingue del resto para poder filtrar
    // los ACKs por separado en la tabla
    private static final String URI_ACK = "webhooks/pagos/ack";

    private final CrmIntegracionLogService crmIntegracionLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Ejecuta la llamada midiendo su duracion y deja una fila en el log con lo enviado y lo que
     * respondio el CRM.
     * <p>
     * Espera: el request, su cuerpo y la cadena de ejecucion.
     * Responde: la respuesta del CRM, sin modificar.
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long inicio = System.currentTimeMillis();
        String requestPayload = new String(body, StandardCharsets.UTF_8);

        try {
            ClientHttpResponse respuesta = execution.execute(request, body);

            int statusCode = respuesta.getStatusCode().value();
            this.registrar(request, requestPayload, statusCode, this.leerCuerpo(respuesta), null,
                    respuesta.getStatusCode().is2xxSuccessful(), System.currentTimeMillis() - inicio);

            return respuesta;
        } catch (IOException | RuntimeException e) {
            this.registrar(request, requestPayload, null, null, e.toString(), false,
                    System.currentTimeMillis() - inicio);
            throw e;
        }
    }

    /** Arma y manda a guardar el registro, sin dejar que un fallo del log corte la llamada. */
    private void registrar(
            HttpRequest request,
            String requestPayload,
            Integer statusCode,
            String responsePayload,
            String errorPayload,
            boolean exitoso,
            long duracionMs) {
        try {
            URI uri = request.getURI();
            String operacion = this.resolverOperacion(uri);

            this.crmIntegracionLogService.registrar(CrmIntegracionLogRegistro.builder()
                    .direccion(CrmLogDireccionEnum.SALIDA)
                    .canal(this.resolverCanal(uri))
                    .operacion(operacion)
                    .eventId(this.leerCampoDelRequest(requestPayload, "eventId"))
                    .tipoEvento(this.leerCampoDelRequest(requestPayload, "tipo"))
                    .metodoHttp(request.getMethod().name())
                    .url(uri.toString())
                    .statusCode(statusCode)
                    .exitoso(exitoso)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .errorPayload(errorPayload)
                    .duracionMs(duracionMs)
                    .build());
        } catch (Exception e) {
            log.warn("CrmLoggingInterceptor | registrar | no se pudo armar el log | error: {}", e.getMessage());
        }
    }

    /**
     * Lee el cuerpo de la respuesta sin romper la llamada si falla.
     * Es seguro releerlo despues porque el RestClient usa un factory con buffering.
     */
    private String leerCuerpo(ClientHttpResponse respuesta) {
        try {
            return StreamUtils.copyToString(respuesta.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("CrmLoggingInterceptor | leerCuerpo | no se pudo leer la respuesta | error: {}",
                    e.getMessage());
            return null;
        }
    }

    /** Distingue los ACKs del resto de las llamadas segun el endpoint. */
    private CrmLogCanalEnum resolverCanal(URI uri) {
        String path = uri.getPath();
        return path != null && path.contains(URI_ACK) ? CrmLogCanalEnum.ACK : CrmLogCanalEnum.API;
    }

    /**
     * Deja el path del endpoint como nombre de la operacion, recortando el context-path del CRM
     * para que quede la parte que identifica la llamada. Ej: integraciones/obtener-planes
     */
    private String resolverOperacion(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        int desde = path.indexOf("integraciones/");
        return desde >= 0 ? path.substring(desde) : path;
    }

    /**
     * Extrae un campo de primer nivel del cuerpo enviado. Sirve para quedarse con el eventId y
     * el tipo del ACK, cuyo payload es {eventId, tipo, ok, data}.
     * <p>
     * Responde null si el cuerpo esta vacio, no es JSON o no tiene ese campo, que es el caso de
     * todas las llamadas que no son ACKs.
     */
    private String leerCampoDelRequest(String requestPayload, String campo) {
        if (requestPayload == null || requestPayload.isBlank()) {
            return null;
        }
        try {
            JsonNode json = this.objectMapper.readTree(requestPayload);
            return json.hasNonNull(campo) ? json.get(campo).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
