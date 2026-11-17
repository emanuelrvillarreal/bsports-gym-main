package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.exceptions.MPApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializa payloads para mercado_pago_traza. No incluir accessToken ni webhookSecret.
 * Las respuestas Payment pueden contener PII del pagador (email, etc.).
 */
@Component
@RequiredArgsConstructor
public class MercadoPagoRawJsonSerializer {

    private final ObjectMapper objectMapper;

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"serializationError\":\"" + escape(ex.getMessage()) + "\"}";
        }
    }

    public String apiExceptionToJson(MPApiException ex) {
        if (ex == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("statusCode", ex.getStatusCode());
        payload.put("message", ex.getMessage());
        if (ex.getApiResponse() != null) {
            payload.put("content", ex.getApiResponse().getContent());
        }
        return toJson(payload);
    }

    public String webhookPayload(String dataIdQuery, String typeQuery, String xRequestId, String xSignature, Object body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, String> query = new LinkedHashMap<>();
        if (dataIdQuery != null && !dataIdQuery.isBlank()) {
            query.put("data.id", dataIdQuery);
        }
        if (typeQuery != null && !typeQuery.isBlank()) {
            query.put("type", typeQuery);
        }
        if (!query.isEmpty()) {
            payload.put("query", query);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        if (xRequestId != null && !xRequestId.isBlank()) {
            headers.put("x-request-id", xRequestId);
        }
        if (xSignature != null && !xSignature.isBlank()) {
            headers.put("x-signature", xSignature);
        }
        if (!headers.isEmpty()) {
            payload.put("headers", headers);
        }

        if (body != null) {
            payload.put("body", body);
        }
        return toJson(payload);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
