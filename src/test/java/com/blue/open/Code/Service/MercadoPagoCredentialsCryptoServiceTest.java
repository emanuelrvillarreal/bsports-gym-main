package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MercadoPagoCredentialsCryptoServiceTest {

    private static final String KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

    @Test
    void encryptDecrypt_roundTrip_conAadCorrecto() {
        MercadoPagoCredentialsCryptoService service = new MercadoPagoCredentialsCryptoService(KEY);

        String encrypted = service.encrypt("APP_USR-secret-1234", "company-uuid",
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD);

        assertNotEquals("APP_USR-secret-1234", encrypted);
        assertEquals("APP_USR-secret-1234", service.decrypt(encrypted, "company-uuid",
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));
    }

    @Test
    void encrypt_usaIvAleatorio() {
        MercadoPagoCredentialsCryptoService service = new MercadoPagoCredentialsCryptoService(KEY);

        String first = service.encrypt("secret", "company-uuid", MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD);
        String second = service.encrypt("secret", "company-uuid", MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD);

        assertNotEquals(first, second);
    }

    @Test
    void decrypt_rechazaAadIncorrectoYCiphertextAlterado() {
        MercadoPagoCredentialsCryptoService service = new MercadoPagoCredentialsCryptoService(KEY);
        String encrypted = service.encrypt("secret", "company-uuid", MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD);

        assertThrows(CustomException.class, () -> service.decrypt(encrypted, "other-company",
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
        int ciphertextStart = encrypted.lastIndexOf(':') + 1;
        char firstCiphertextCharacter = encrypted.charAt(ciphertextStart);
        String altered = encrypted.substring(0, ciphertextStart)
                + (firstCiphertextCharacter == 'A' ? 'B' : 'A')
                + encrypted.substring(ciphertextStart + 1);
        assertThrows(CustomException.class, () -> service.decrypt(altered, "company-uuid",
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
    }

    @Test
    void constructor_rechazaClaveInvalida() {
        assertThrows(IllegalStateException.class, () -> new MercadoPagoCredentialsCryptoService("not-base64"));
        assertThrows(IllegalStateException.class, () -> new MercadoPagoCredentialsCryptoService("AAECAwQ="));
        assertDoesNotThrow(() -> new MercadoPagoCredentialsCryptoService(KEY));
    }
}
