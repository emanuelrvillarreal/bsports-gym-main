package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MercadoPagoWebhookNotificationDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Service.MercadoPagoCredentialsCryptoService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoRawJsonSerializer;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoTrazaService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoWebhookProcessor;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoWebhookService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoWebhookSignatureValidator;
import com.blue.open.Core.Security.JacksonConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookServiceTest {

    @Mock
    private CompanyMercadoPagoRepository companyMercadoPagoRepository;

    @Mock
    private MercadoPagoWebhookSignatureValidator signatureValidator;

    @Mock
    private MercadoPagoWebhookProcessor mercadoPagoWebhookProcessor;

    private MercadoPagoTrazaService mercadoPagoTrazaService;
    private MercadoPagoRawJsonSerializer rawJsonSerializer;
    private MercadoPagoCredentialsCryptoService credentialsCryptoService;

    private MercadoPagoWebhookService service;

    @BeforeEach
    void setUp() {
        mercadoPagoTrazaService = new NoOpMercadoPagoTrazaService();
        rawJsonSerializer = new MercadoPagoRawJsonSerializer(new JacksonConfig().objectMapper());
        credentialsCryptoService = new MercadoPagoCredentialsCryptoService(
                "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
        service = new MercadoPagoWebhookService(
                companyMercadoPagoRepository,
                signatureValidator,
                mercadoPagoWebhookProcessor,
                mercadoPagoTrazaService,
                rawJsonSerializer);
        ReflectionTestUtils.setField(service, "credentialsCryptoService", credentialsCryptoService);
    }

    @Test
    void procesarNotificacion_payment_encolaProcesamientoAsync() {
        Company company = new Company();
        company.setId(1L);
        company.setUuid("club-uuid");

        CompanyMercadoPago config = new CompanyMercadoPago();
        config.setCompany(company);
        config.setWebhookSecret(credentialsCryptoService.encrypt("secret", company.getUuid(),
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));

        MercadoPagoWebhookNotificationDTO body = new MercadoPagoWebhookNotificationDTO();
        body.setType("payment");
        body.setAction("payment.created");
        MercadoPagoWebhookNotificationDTO.WebhookData data = new MercadoPagoWebhookNotificationDTO.WebhookData();
        data.setId("12345");
        body.setData(data);

        when(companyMercadoPagoRepository.findActiveByCompanyUuid("club-uuid")).thenReturn(Optional.of(config));

        service.procesarNotificacion("club-uuid", "sig", "req-1", null, "payment", body);

        verify(signatureValidator).validate("secret", "sig", "req-1", null);
        verify(mercadoPagoWebhookProcessor).procesarPagoAsync("club-uuid", "12345", "payment", "payment.created");
    }

    @Test
    void procesarNotificacion_otroTipo_noEncolaAsync() {
        Company company = new Company();
        company.setId(1L);
        company.setUuid("club-uuid");

        CompanyMercadoPago config = new CompanyMercadoPago();
        config.setCompany(company);
        config.setWebhookSecret(credentialsCryptoService.encrypt("secret", company.getUuid(),
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));

        MercadoPagoWebhookNotificationDTO body = new MercadoPagoWebhookNotificationDTO();
        body.setType("merchant_order");
        MercadoPagoWebhookNotificationDTO.WebhookData data = new MercadoPagoWebhookNotificationDTO.WebhookData();
        data.setId("999");
        body.setData(data);

        when(companyMercadoPagoRepository.findActiveByCompanyUuid("club-uuid")).thenReturn(Optional.of(config));

        service.procesarNotificacion("club-uuid", "sig", "req-1", null, "merchant_order", body);

        verify(mercadoPagoWebhookProcessor, never()).procesarPagoAsync(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static class NoOpMercadoPagoTrazaService extends MercadoPagoTrazaService {

        NoOpMercadoPagoTrazaService() {
            super(null);
        }

        @Override
        public void registrar(com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro registro) {
        }
    }
}
