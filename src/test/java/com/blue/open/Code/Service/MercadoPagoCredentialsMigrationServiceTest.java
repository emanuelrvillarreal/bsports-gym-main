package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoCredentialsMigrationServiceTest {

    @Mock
    private CompanyMercadoPagoRepository companyMercadoPagoRepository;

    private MercadoPagoCredentialsCryptoService credentialsCryptoService;
    private MercadoPagoCredentialsMigrationService migrationService;

    @BeforeEach
    void setUp() {
        credentialsCryptoService = new MercadoPagoCredentialsCryptoService(
                "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
        migrationService = new MercadoPagoCredentialsMigrationService(
                companyMercadoPagoRepository, credentialsCryptoService);
    }

    @Test
    void migrateLegacyCredentials_cifraSoloCredencialesEnClaroEIncluyeInactivas() {
        CompanyMercadoPago legacy = config("legacy-company", "token-1234", "webhook-abcd");
        legacy.setStatus(1);
        CompanyMercadoPago empty = config("empty-company", null, null);
        CompanyMercadoPago encrypted = config("encrypted-company",
                credentialsCryptoService.encrypt("already-encrypted", "encrypted-company",
                        MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD),
                credentialsCryptoService.encrypt("already-encrypted", "encrypted-company",
                        MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));

        when(companyMercadoPagoRepository.findAllIncludingInactiveWithCompany())
                .thenReturn(List.of(legacy, empty, encrypted));

        migrationService.migrateLegacyCredentials();

        assertNotEquals("token-1234", legacy.getAccessToken());
        assertEquals("token-1234", credentialsCryptoService.decrypt(legacy.getAccessToken(), "legacy-company",
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));
        assertEquals("webhook-abcd", credentialsCryptoService.decrypt(legacy.getWebhookSecret(), "legacy-company",
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
        assertNull(empty.getAccessToken());
        assertNull(empty.getWebhookSecret());
        assertEquals("already-encrypted", credentialsCryptoService.decrypt(encrypted.getAccessToken(), "encrypted-company",
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));

        ArgumentCaptor<List<CompanyMercadoPago>> saved = ArgumentCaptor.forClass(List.class);
        verify(companyMercadoPagoRepository).saveAll(saved.capture());
        assertEquals(List.of(legacy), saved.getValue());
    }

    @Test
    void migrateLegacyCredentials_esIdempotente() {
        CompanyMercadoPago encrypted = config("company",
                credentialsCryptoService.encrypt("token", "company", MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD),
                credentialsCryptoService.encrypt("secret", "company", MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
        when(companyMercadoPagoRepository.findAllIncludingInactiveWithCompany()).thenReturn(List.of(encrypted));

        migrationService.migrateLegacyCredentials();

        verify(companyMercadoPagoRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private CompanyMercadoPago config(String companyUuid, String accessToken, String webhookSecret) {
        Company company = new Company();
        company.setUuid(companyUuid);
        CompanyMercadoPago config = CompanyMercadoPago.crearDeshabilitadoVacias(company);
        config.setAccessToken(accessToken);
        config.setWebhookSecret(webhookSecret);
        return config;
    }
}
