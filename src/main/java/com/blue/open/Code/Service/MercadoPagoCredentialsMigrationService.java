package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MercadoPagoCredentialsMigrationService implements ApplicationRunner {

    private final CompanyMercadoPagoRepository companyMercadoPagoRepository;
    private final MercadoPagoCredentialsCryptoService credentialsCryptoService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateLegacyCredentials();
    }

    @Transactional
    public void migrateLegacyCredentials() {
        List<CompanyMercadoPago> configs = companyMercadoPagoRepository.findAllIncludingInactiveWithCompany();
        List<CompanyMercadoPago> migratedConfigs = new ArrayList<>();
        for (CompanyMercadoPago config : configs) {
            String companyUuid = config.getCompany().getUuid();
            boolean accessTokenMigrated = encryptIfPlaintext(config, companyUuid,
                    MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD);
            boolean webhookSecretMigrated = encryptIfPlaintext(config, companyUuid,
                    MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD);
            if (accessTokenMigrated || webhookSecretMigrated) {
                migratedConfigs.add(config);
            }
        }
        if (!migratedConfigs.isEmpty()) {
            companyMercadoPagoRepository.saveAll(migratedConfigs);
        }
    }

    private boolean encryptIfPlaintext(CompanyMercadoPago config, String companyUuid, String field) {
        String value = MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD.equals(field)
                ? config.getAccessToken() : config.getWebhookSecret();
        if (value == null || credentialsCryptoService.isEncrypted(value)) {
            return false;
        }
        String encrypted = credentialsCryptoService.encrypt(value, companyUuid, field);
        if (MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD.equals(field)) {
            config.setAccessToken(encrypted);
        } else {
            config.setWebhookSecret(encrypted);
        }
        return true;
    }
}
