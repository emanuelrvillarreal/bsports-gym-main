package com.blue.open.Code.Entity.DTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysadminMercadoPagoConfigRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validacion_accessTokenVacio_falla() {
        SysadminMercadoPagoConfigRequestDTO dto = new SysadminMercadoPagoConfigRequestDTO();
        dto.setAccessToken("   ");

        Set<ConstraintViolation<SysadminMercadoPagoConfigRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("access token")));
    }

    @Test
    void validacion_webhookSecretVacio_falla() {
        SysadminMercadoPagoConfigRequestDTO dto = new SysadminMercadoPagoConfigRequestDTO();
        dto.setWebhookSecret("");

        Set<ConstraintViolation<SysadminMercadoPagoConfigRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("webhook secret")));
    }

    @Test
    void validacion_partialUpdate_sinCampos_pasa() {
        SysadminMercadoPagoConfigRequestDTO dto = new SysadminMercadoPagoConfigRequestDTO();
        dto.setHabilitado(false);

        Set<ConstraintViolation<SysadminMercadoPagoConfigRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validacion_accessTokenDemasiadoLargo_falla() {
        SysadminMercadoPagoConfigRequestDTO dto = new SysadminMercadoPagoConfigRequestDTO();
        dto.setAccessToken("x".repeat(513));

        Set<ConstraintViolation<SysadminMercadoPagoConfigRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Access token")));
    }
}
