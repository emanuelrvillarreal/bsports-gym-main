package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigResponseDTO;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import org.springframework.stereotype.Service;

@Service
public class CompanyMercadoPagoMapper {

    public SysadminMercadoPagoConfigResponseDTO modelToResponseDto(
            CompanyMercadoPago config, String accessToken, String webhookSecret) {
        SysadminMercadoPagoConfigResponseDTO dto = new SysadminMercadoPagoConfigResponseDTO();
        dto.setCompanyUuid(config.getCompany().getUuid());
        dto.setHabilitado(config.getHabilitado());
        dto.setOperativo(config.isOperativo());
        dto.setWebhookOperativo(config.isWebhookOperativo());
        dto.setAccessTokenConfigured(isConfigured(accessToken));
        dto.setAccessTokenHint(maskSecret(accessToken));
        dto.setPublicKey(config.getPublicKey());
        dto.setWebhookSecretConfigured(isConfigured(webhookSecret));
        dto.setWebhookSecretHint(maskSecret(webhookSecret));
        return dto;
    }

    public void applyRequest(CompanyMercadoPago config, SysadminMercadoPagoConfigRequestDTO request) {
        if (request.getPublicKey() != null) {
            config.setPublicKey(request.getPublicKey().isBlank() ? null : request.getPublicKey().trim());
        }
    }

    public void applyHabilitadoRule(CompanyMercadoPago config, SysadminMercadoPagoConfigRequestDTO request) {
        if (Boolean.FALSE.equals(request.getHabilitado())) {
            config.setHabilitado(false);
        } else if (tieneAccessTokenValido(config)) {
            config.setHabilitado(true);
        } else {
            config.setHabilitado(false);
        }
    }

    public static String maskSecret(String value) {
        if (!isConfigured(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean tieneAccessTokenValido(CompanyMercadoPago config) {
        return isConfigured(config.getAccessToken());
    }
}
