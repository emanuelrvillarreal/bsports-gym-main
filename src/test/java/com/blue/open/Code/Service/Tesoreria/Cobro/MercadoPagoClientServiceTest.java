package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Service.MercadoPagoCredentialsCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MercadoPagoClientServiceTest {

    @Test
    void decryptAccessToken_entregaElValorPlanoAlClienteMercadoPago() {
        MercadoPagoCredentialsCryptoService cryptoService = new MercadoPagoCredentialsCryptoService(
                "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
        MercadoPagoClientService clientService = new MercadoPagoClientService(null, null, null);
        ReflectionTestUtils.setField(clientService, "credentialsCryptoService", cryptoService);

        Company company = new Company();
        company.setUuid("club-uuid");
        CompanyMercadoPago config = CompanyMercadoPago.crearDeshabilitadoVacias(company);
        config.setAccessToken(cryptoService.encrypt("APP_USR-token", company.getUuid(),
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));

        assertEquals("APP_USR-token", clientService.decryptAccessToken(config, company));
    }
}
