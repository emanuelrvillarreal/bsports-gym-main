package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.DTO.MercadoPagoWebhookNotificationDTO;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Service.MercadoPagoCredentialsCryptoService;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookService {

    private final CompanyMercadoPagoRepository companyMercadoPagoRepository;
    private final MercadoPagoWebhookSignatureValidator signatureValidator;
    private final MercadoPagoWebhookProcessor mercadoPagoWebhookProcessor;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;

    @Autowired
    private MercadoPagoCredentialsCryptoService credentialsCryptoService;

    public void procesarNotificacion(
            String companyUuid,
            String xSignature,
            String xRequestId,
            String dataIdQuery,
            String typeQuery,
            MercadoPagoWebhookNotificationDTO body) {

        CompanyMercadoPago config = companyMercadoPagoRepository.findActiveByCompanyUuid(companyUuid)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Gimnasio no encontrado",
                        "MercadoPagoWebhookService | companyUuid: " + companyUuid));

        if (!config.isWebhookOperativo()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Webhook de Mercado Pago no configurado para este gimnasio",
                    "MercadoPagoWebhookService | companyId: " + config.getCompany().getId());
        }

        String dataIdForSignature = resolveDataIdForSignature(dataIdQuery);
        String dataId = resolveDataId(dataIdQuery, body);

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(config.getCompany())
                .etapa(MercadoPagoTrazaEtapaEnum.WEBHOOK_RECIBIDO)
                .paymentId(parsePaymentId(dataId))
                .requestRaw(rawJsonSerializer.webhookPayload(dataIdQuery, typeQuery, xRequestId, xSignature, body))
                .mensaje("Webhook MP recibido | type=" + resolveType(typeQuery, body)
                        + " | action=" + (body != null ? body.getAction() : null)
                        + " | dataIdFirma=" + dataIdForSignature)
                .build());

        try {
            signatureValidator.validate(credentialsCryptoService.decrypt(config.getWebhookSecret(),
                    companyUuid, MercadoPagoCredentialsCryptoService.WEBHOOK_SECRET_FIELD),
                    xSignature, xRequestId, dataIdForSignature);
        } catch (CustomException ex) {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(config.getCompany())
                    .etapa(MercadoPagoTrazaEtapaEnum.WEBHOOK_FIRMA_INVALIDA)
                    .nivel(MercadoPagoTrazaNivelEnum.WARN)
                    .paymentId(parsePaymentId(dataId))
                    .requestRaw(rawJsonSerializer.webhookPayload(dataIdQuery, typeQuery, xRequestId, xSignature, body))
                    .mensaje("Firma de webhook MP inválida | dataIdFirma=" + dataIdForSignature
                            + " | " + ex.getDeveloperMessage())
                    .build());
            throw ex;
        }

        String type = resolveType(typeQuery, body);
        String action = body != null ? body.getAction() : null;
        String resourceId = body != null && body.getData() != null ? body.getData().getId() : dataId;

        if (isPaymentNotification(type)) {
            mercadoPagoWebhookProcessor.procesarPagoAsync(companyUuid, resourceId, type, action);
        } else {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(config.getCompany())
                    .etapa(MercadoPagoTrazaEtapaEnum.WEBHOOK_TIPO_IGNORADO)
                    .nivel(MercadoPagoTrazaNivelEnum.WARN)
                    .paymentId(parsePaymentId(resourceId))
                    .mensaje("Webhook MP ignorado | type=" + type)
                    .build());
        }
    }

    private boolean isPaymentNotification(String type) {
        return type != null && "payment".equalsIgnoreCase(type);
    }

    private String resolveDataIdForSignature(String dataIdQuery) {
        if (dataIdQuery == null || dataIdQuery.isBlank()) {
            return null;
        }
        return dataIdQuery.trim();
    }

    private String resolveDataId(String dataIdQuery, MercadoPagoWebhookNotificationDTO body) {
        if (dataIdQuery != null && !dataIdQuery.isBlank()) {
            return dataIdQuery;
        }
        if (body != null && body.getData() != null && body.getData().getId() != null) {
            return body.getData().getId();
        }
        return null;
    }

    private String resolveType(String typeQuery, MercadoPagoWebhookNotificationDTO body) {
        if (typeQuery != null && !typeQuery.isBlank()) {
            return typeQuery;
        }
        return body != null ? body.getType() : null;
    }

    private Long parsePaymentId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
