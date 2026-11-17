package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MercadoPagoWebhookNotificationDTO;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoRawJsonSerializer;
import com.blue.open.Core.Security.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.MPResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoRawJsonSerializerTest {

    private MercadoPagoRawJsonSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JacksonConfig().objectMapper();
        serializer = new MercadoPagoRawJsonSerializer(objectMapper);
    }

    @Test
    void toJson_nullRetornaNull() {
        assertNull(serializer.toJson(null));
    }

    @Test
    void webhookPayload_incluyeQueryHeadersYBody() {
        MercadoPagoWebhookNotificationDTO body = new MercadoPagoWebhookNotificationDTO();
        body.setType("payment");
        MercadoPagoWebhookNotificationDTO.WebhookData data = new MercadoPagoWebhookNotificationDTO.WebhookData();
        data.setId("123");
        body.setData(data);

        String json = serializer.webhookPayload("123", "payment", "req-1", "ts=1,v1=abc", body);

        assertTrue(json.contains("\"data.id\":\"123\""));
        assertTrue(json.contains("\"x-request-id\":\"req-1\""));
        assertTrue(json.contains("\"type\":\"payment\""));
    }

    @Test
    void apiExceptionToJson_incluyeStatusYContent() throws Exception {
        MPResponse response = new MPResponse(400, null, "{\"error\":\"bad_request\"}");
        MPApiException ex = new MPApiException("api error", response);

        String json = serializer.apiExceptionToJson(ex);

        assertTrue(json.contains("\"statusCode\":400"));
        assertTrue(json.contains("\"content\":\"{\\\"error\\\":\\\"bad_request\\\"}\""));
    }
}
