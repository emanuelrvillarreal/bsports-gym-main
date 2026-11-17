package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookProcessor {

    private final MercadoPagoWebhookLiquidacionService liquidacionService;
    private final CompanyMercadoPagoRepository companyMercadoPagoRepository;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;

    @Async("AsyncTaskExecutor")
    public void procesarPagoAsync(String companyUuid, String paymentIdStr, String type, String action) {
        if (paymentIdStr == null || paymentIdStr.isBlank()) {
            registrarTraza(companyUuid, null, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR,
                    MercadoPagoTrazaNivelEnum.WARN, null, "Webhook async sin paymentId | type=" + type);
            return;
        }

        Long paymentId = null;
        try {
            paymentId = Long.parseLong(paymentIdStr);
            registrarTraza(companyUuid, paymentId, MercadoPagoTrazaEtapaEnum.WEBHOOK_ENCOLADO,
                    MercadoPagoTrazaNivelEnum.INFO, null,
                    "Webhook MP encolado para liquidación | type=" + type + " | action=" + action);
            liquidacionService.procesarPago(companyUuid, paymentId);
        } catch (NumberFormatException ex) {
            registrarTraza(companyUuid, paymentId, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR,
                    MercadoPagoTrazaNivelEnum.WARN, null,
                    "Webhook async paymentId inválido | paymentId=" + paymentIdStr);
        } catch (Exception ex) {
            registrarTraza(companyUuid, paymentId, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR,
                    MercadoPagoTrazaNivelEnum.ERROR, null,
                    "Webhook async error | paymentId=" + paymentIdStr + " | " + ex.getMessage());
            log.error("MP webhook async error | companyUuid={} | paymentId={} | type={} | action={}",
                    companyUuid, paymentIdStr, type, action, ex);
        }
    }

    private void registrarTraza(
            String companyUuid,
            Long paymentId,
            MercadoPagoTrazaEtapaEnum etapa,
            MercadoPagoTrazaNivelEnum nivel,
            String externalReference,
            String mensaje) {
        companyMercadoPagoRepository.findActiveByCompanyUuid(companyUuid)
                .map(config -> config.getCompany())
                .ifPresent(company -> mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                        .company(company)
                        .etapa(etapa)
                        .nivel(nivel)
                        .paymentId(paymentId)
                        .externalReference(externalReference)
                        .mensaje(mensaje)
                        .build()));
    }
}
