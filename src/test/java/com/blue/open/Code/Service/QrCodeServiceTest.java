package com.blue.open.Code.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void generateBase64Png_devuelveImagenBase64() {
        String qr = service.generateBase64Png("550e8400-e29b-41d4-a716-446655440000");

        assertNotNull(qr);
        assertFalse(qr.isBlank());
        assertTrue(qr.matches("^[A-Za-z0-9+/=]+$"));
    }
}
