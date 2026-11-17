package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigResponseDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Mapper.CompanyMercadoPagoMapper;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysadminMercadoPagoConfigServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMercadoPagoRepository companyMercadoPagoRepository;

    private CompanyMercadoPagoMapper companyMercadoPagoMapper;
    private MercadoPagoCredentialsCryptoService credentialsCryptoService;
    private SysadminMercadoPagoConfigService service;

    @BeforeEach
    void setUp() {
        companyMercadoPagoMapper = new CompanyMercadoPagoMapper();
        credentialsCryptoService = new MercadoPagoCredentialsCryptoService(
                "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
        service = new SysadminMercadoPagoConfigService();
        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(service, "companyMercadoPagoRepository", companyMercadoPagoRepository);
        ReflectionTestUtils.setField(service, "companyMercadoPagoMapper", companyMercadoPagoMapper);
        ReflectionTestUtils.setField(service, "credentialsCryptoService", credentialsCryptoService);
    }

    @Test
    void guardar_conAccessToken_habilitaYOperativo() {
        Company company = company("club-uuid", 1L);
        CompanyMercadoPago config = config(company, false, null, null, null);

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyMercadoPagoRepository.findActiveByCompanyId(1L)).thenReturn(Optional.of(config));
        when(companyMercadoPagoRepository.save(any(CompanyMercadoPago.class))).thenAnswer(inv -> inv.getArgument(0));

        SysadminMercadoPagoConfigRequestDTO request = new SysadminMercadoPagoConfigRequestDTO();
        request.setAccessToken("APP_USR-token-1234");
        request.setPublicKey("APP_USR-public-key");
        request.setWebhookSecret("whsec-5678abcd");

        SysadminMercadoPagoConfigResponseDTO response = service.guardar("club-uuid", request);

        assertTrue(response.getHabilitado());
        assertTrue(response.isOperativo());
        assertTrue(response.isWebhookOperativo());
        assertTrue(response.isAccessTokenConfigured());
        assertEquals("****1234", response.getAccessTokenHint());
        assertEquals("APP_USR-public-key", response.getPublicKey());
        assertEquals("****abcd", response.getWebhookSecretHint());
        assertNotEquals("APP_USR-token-1234", response.getAccessTokenHint());
    }

    @Test
    void guardar_habilitadoFalse_deshabilitaAunqueHayaToken() {
        Company company = company("club-uuid", 1L);
        CompanyMercadoPago config = config(company, true, "APP_USR-existing", "pk", "secret");

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyMercadoPagoRepository.findActiveByCompanyId(1L)).thenReturn(Optional.of(config));
        when(companyMercadoPagoRepository.save(any(CompanyMercadoPago.class))).thenAnswer(inv -> inv.getArgument(0));

        SysadminMercadoPagoConfigRequestDTO request = new SysadminMercadoPagoConfigRequestDTO();
        request.setHabilitado(false);

        SysadminMercadoPagoConfigResponseDTO response = service.guardar("club-uuid", request);

        assertFalse(response.getHabilitado());
        assertFalse(response.isOperativo());
    }

    @Test
    void guardar_soloWebhookSecret_noCambiaTokenNiHabilitado() {
        Company company = company("club-uuid", 1L);
        CompanyMercadoPago config = config(company, true, "APP_USR-existing", "pk", null);

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyMercadoPagoRepository.findActiveByCompanyId(1L)).thenReturn(Optional.of(config));
        when(companyMercadoPagoRepository.save(any(CompanyMercadoPago.class))).thenAnswer(inv -> inv.getArgument(0));

        SysadminMercadoPagoConfigRequestDTO request = new SysadminMercadoPagoConfigRequestDTO();
        request.setWebhookSecret("new-webhook-secret-abcd");

        SysadminMercadoPagoConfigResponseDTO response = service.guardar("club-uuid", request);

        assertTrue(response.getHabilitado());
        assertEquals("APP_USR-existing", decryptAccessToken(config));
        assertTrue(response.isWebhookOperativo());
        assertEquals("****abcd", response.getWebhookSecretHint());
    }

    @Test
    void obtener_devuelveConfigEnmascarada() {
        Company company = company("club-uuid", 1L);
        CompanyMercadoPago config = config(company, true, "APP_USR-token-9999", "APP_USR-pk", "whsec-1111zzzz");

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyMercadoPagoRepository.findActiveByCompanyId(1L)).thenReturn(Optional.of(config));

        SysadminMercadoPagoConfigResponseDTO response = service.obtener("club-uuid");

        assertEquals("club-uuid", response.getCompanyUuid());
        assertTrue(response.isOperativo());
        assertEquals("****9999", response.getAccessTokenHint());
        assertEquals("****zzzz", response.getWebhookSecretHint());
        assertNotEquals("APP_USR-token-9999", response.getAccessTokenHint());
    }

    @Test
    void guardar_clubInexistente_lanza404() {
        when(companyRepository.findFirstByUuidWithStatusIsActive("missing")).thenReturn(null);

        SysadminMercadoPagoConfigRequestDTO request = new SysadminMercadoPagoConfigRequestDTO();
        request.setAccessToken("APP_USR-token");

        CustomException ex = assertThrows(CustomException.class,
                () -> service.guardar("missing", request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(companyMercadoPagoRepository, never()).save(any());
    }

    @Test
    void guardar_sinConfigExistente_creaYGuarda() {
        Company company = company("club-uuid", 1L);

        when(companyRepository.findFirstByUuidWithStatusIsActive("club-uuid")).thenReturn(company);
        when(companyMercadoPagoRepository.findActiveByCompanyId(1L)).thenReturn(Optional.empty());
        when(companyMercadoPagoRepository.save(any(CompanyMercadoPago.class))).thenAnswer(inv -> inv.getArgument(0));

        SysadminMercadoPagoConfigRequestDTO request = new SysadminMercadoPagoConfigRequestDTO();
        request.setAccessToken("APP_USR-new-token-abcd");

        service.guardar("club-uuid", request);

        ArgumentCaptor<CompanyMercadoPago> captor = ArgumentCaptor.forClass(CompanyMercadoPago.class);
        verify(companyMercadoPagoRepository).save(captor.capture());
        assertNotEquals("APP_USR-new-token-abcd", captor.getValue().getAccessToken());
        assertEquals("APP_USR-new-token-abcd", decryptAccessToken(captor.getValue()));
        assertTrue(captor.getValue().getHabilitado());
    }

    private Company company(String uuid, Long id) {
        Company company = new Company();
        company.setId(id);
        company.setUuid(uuid);
        return company;
    }

    private CompanyMercadoPago config(
            Company company,
            boolean habilitado,
            String accessToken,
            String publicKey,
            String webhookSecret) {
        CompanyMercadoPago config = CompanyMercadoPago.crearDeshabilitadoVacias(company);
        config.setHabilitado(habilitado);
        config.setAccessToken(accessToken == null ? null : credentialsCryptoService.encrypt(accessToken, company.getUuid(),
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));
        config.setPublicKey(publicKey);
        config.setWebhookSecret(webhookSecret == null ? null : credentialsCryptoService.encrypt(webhookSecret, company.getUuid(),
                MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
        return config;
    }

    private String decryptAccessToken(CompanyMercadoPago config) {
        return credentialsCryptoService.decrypt(config.getAccessToken(), config.getCompany().getUuid(),
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD);
    }
}
