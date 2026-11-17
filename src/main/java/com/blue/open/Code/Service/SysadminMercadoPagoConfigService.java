package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigResponseDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Mapper.CompanyMercadoPagoMapper;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysadminMercadoPagoConfigService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMercadoPagoRepository companyMercadoPagoRepository;

    @Autowired
    private CompanyMercadoPagoMapper companyMercadoPagoMapper;

    @Autowired
    private MercadoPagoCredentialsCryptoService credentialsCryptoService;

    @Transactional(readOnly = true)
    public SysadminMercadoPagoConfigResponseDTO obtener(String companyUuid) {
        Company company = resolveCompany(companyUuid);
        CompanyMercadoPago config = resolveOrCreateConfig(company);
        return toResponse(config);
    }

    @Transactional
    public SysadminMercadoPagoConfigResponseDTO guardar(
            String companyUuid, SysadminMercadoPagoConfigRequestDTO request) {
        Company company = resolveCompany(companyUuid);
        CompanyMercadoPago config = resolveOrCreateConfig(company);

        companyMercadoPagoMapper.applyRequest(config, request);
        encryptSecretsFromRequest(config, request);
        companyMercadoPagoMapper.applyHabilitadoRule(config, request);

        CompanyMercadoPago saved = companyMercadoPagoRepository.save(config);
        return toResponse(saved);
    }

    private Company resolveCompany(String companyUuid) {
        if (companyUuid == null || companyUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Gimnasio requerido",
                    "SysadminMercadoPagoConfigService | companyUuid vacío");
        }
        Company company = companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
        if (company == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                    "SysadminMercadoPagoConfigService | companyUuid: " + companyUuid);
        }
        return company;
    }

    private CompanyMercadoPago resolveOrCreateConfig(Company company) {
        return companyMercadoPagoRepository.findActiveByCompanyId(company.getId())
                .orElseGet(() -> {
                    CompanyMercadoPago config = CompanyMercadoPago.crearDeshabilitadoVacias(company);
                    company.setMercadoPago(config);
                    return config;
                });
    }

    private void encryptSecretsFromRequest(
            CompanyMercadoPago config, SysadminMercadoPagoConfigRequestDTO request) {
        String companyUuid = config.getCompany().getUuid();
        if (request.getAccessToken() != null) {
            config.setAccessToken(credentialsCryptoService.encrypt(request.getAccessToken().trim(), companyUuid,
                    MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD));
        }
        if (request.getWebhookSecret() != null) {
            config.setWebhookSecret(credentialsCryptoService.encrypt(request.getWebhookSecret().trim(), companyUuid,
                    MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
        }
    }

    private SysadminMercadoPagoConfigResponseDTO toResponse(CompanyMercadoPago config) {
        String companyUuid = config.getCompany().getUuid();
        return companyMercadoPagoMapper.modelToResponseDto(
                config,
                credentialsCryptoService.decrypt(config.getAccessToken(), companyUuid,
                        MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD),
                credentialsCryptoService.decrypt(config.getWebhookSecret(), companyUuid,
                        MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD));
    }
}
