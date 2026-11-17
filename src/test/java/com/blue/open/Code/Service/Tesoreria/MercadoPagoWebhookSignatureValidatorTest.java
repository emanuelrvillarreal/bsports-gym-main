package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoWebhookSignatureValidator;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MercadoPagoWebhookSignatureValidatorTest {

    private static final String SECRET = "test-webhook-secret";

    private MercadoPagoWebhookSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MercadoPagoWebhookSignatureValidator();
    }

    @Test
    void validate_aceptaFirmaValida() {
        String dataId = "123456";
        String xRequestId = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
        String ts = "1742505638683";
        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String v1 = hmacSha256Hex(SECRET, manifest);
        String xSignature = "ts=" + ts + ",v1=" + v1;

        assertDoesNotThrow(() -> validator.validate(SECRET, xSignature, xRequestId, dataId));
    }

    @Test
    void validate_normalizaDataIdMayusculas() {
        String dataId = "ORD01JQ4S4KY8HWQ6NA5PXB65B3D3";
        String normalized = dataId.toLowerCase();
        String xRequestId = "req-1";
        String ts = "1000";
        String manifest = "id:" + normalized + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String v1 = hmacSha256Hex(SECRET, manifest);
        String xSignature = "ts=" + ts + ",v1=" + v1;

        assertDoesNotThrow(() -> validator.validate(SECRET, xSignature, xRequestId, dataId));
    }

    @Test
    void validate_rechazaFirmaInvalida() {
        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validate(SECRET, "ts=1,v1=invalid", "req-1", "123"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void validate_rechazaSinSecret() {
        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validate("", "ts=1,v1=abc", "req-1", "123"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    private String hmacSha256Hex(String secret, String manifest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
