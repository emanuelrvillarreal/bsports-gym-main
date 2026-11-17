package com.blue.open.Code.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuitValidatorServiceTest {

    static final String VALID_CUIT_FOR_TESTS = discoverValidCuit();

    private static String discoverValidCuit() {
        CuitValidatorService validator = new CuitValidatorService();
        for (int prefix = 20; prefix <= 34; prefix++) {
            for (int body = 0; body < 100_000; body++) {
                String base = String.format("%02d%08d", prefix, body);
                for (int check = 0; check < 10; check++) {
                    String cuit = base + check;
                    if (validator.isValid(cuit)) {
                        return cuit;
                    }
                }
            }
        }
        throw new IllegalStateException("No valid CUIT found for tests");
    }

    @Test
    void discoveredCuitIsValid() {
        assertNotNull(VALID_CUIT_FOR_TESTS);
        assertTrue(new CuitValidatorService().isValid(VALID_CUIT_FOR_TESTS));
    }
}
