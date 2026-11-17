package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Repository.MercadoPagoCobroIntentRepository;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookLiquidacionService {

    private static final String PAYMENT_STATUS_APPROVED = "approved";
    private static final String PAYMENT_STATUS_REJECTED = "rejected";
    private static final String PAYMENT_STATUS_CANCELLED = "cancelled";

    private final CompanyMercadoPagoRepository companyMercadoPagoRepository;
    private final MercadoPagoClientService mercadoPagoClientService;
    private final MercadoPagoCobroIntentRepository mercadoPagoCobroIntentRepository;
    private final MercadoPagoLiquidacionStrategyRegistry liquidacionStrategyRegistry;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;

    @Transactional
    public void procesarPago(String companyUuid, Long paymentId) {
        CompanyMercadoPago config = companyMercadoPagoRepository.findActiveByCompanyUuid(companyUuid)
                .filter(CompanyMercadoPago::isOperativo)
                .orElse(null);
        if (config == null) {
            log.warn("MP liquidación async: club no operativo | companyUuid={}", companyUuid);
            return;
        }

        Company company = config.getCompany();
        Payment payment = mercadoPagoClientService.getPayment(company, paymentId);
        String externalReference = payment.getExternalReference();
        if (externalReference == null || externalReference.isBlank()) {
            registrar(company, null, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR, MercadoPagoTrazaNivelEnum.WARN,
                    null, null, paymentId, "Payment sin external_reference");
            return;
        }

        MercadoPagoCobroIntent intent = mercadoPagoCobroIntentRepository
                .findActiveForUpdateByExternalReferenceAndCompanyId(externalReference, company.getId())
                .orElse(null);
        if (intent == null) {
            registrar(company, null, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR, MercadoPagoTrazaNivelEnum.WARN,
                    externalReference, null, paymentId, "Intent no encontrado");
            return;
        }

        if (intent.getEstado() == MercadoPagoCobroIntentEstadoEnum.APROBADO) {
            registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_IDEMPOTENTE, MercadoPagoTrazaNivelEnum.INFO,
                    externalReference, intent.getPreferenceId(), paymentId, "Intent ya aprobado (idempotente)");
            return;
        }

        intent.setPaymentId(payment.getId());
        String paymentStatus = payment.getStatus();
        var mercadoPagoCobro = intent.getMercadoPagoCobro();

        if (PAYMENT_STATUS_APPROVED.equals(paymentStatus)) {
            if (!montoCoincide(mercadoPagoCobro.getMontoTotal(), payment.getTransactionAmount())) {
                registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_ERROR, MercadoPagoTrazaNivelEnum.ERROR,
                        externalReference, intent.getPreferenceId(), paymentId,
                        "Monto no coincide | esperado=" + mercadoPagoCobro.getMontoTotal()
                                + " | recibido=" + payment.getTransactionAmount());
                return;
            }

            liquidacionStrategyRegistry.get(mercadoPagoCobro.getTipo()).liquidar(intent);
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.APROBADO);
            mercadoPagoCobroIntentRepository.save(intent);
            registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_APROBADA, MercadoPagoTrazaNivelEnum.INFO,
                    externalReference, intent.getPreferenceId(), paymentId,
                    mensajeLiquidacionAprobada(mercadoPagoCobro.getTipo(), paymentId, mercadoPagoCobro.getItems().size()));
        } else if (PAYMENT_STATUS_REJECTED.equals(paymentStatus)) {
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.RECHAZADO);
            mercadoPagoCobroIntentRepository.save(intent);
            registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_RECHAZADA, MercadoPagoTrazaNivelEnum.INFO,
                    externalReference, intent.getPreferenceId(), paymentId, "Pago rechazado");
        } else if (PAYMENT_STATUS_CANCELLED.equals(paymentStatus)) {
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.CANCELADO);
            mercadoPagoCobroIntentRepository.save(intent);
            registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_CANCELADA, MercadoPagoTrazaNivelEnum.INFO,
                    externalReference, intent.getPreferenceId(), paymentId, "Pago cancelado");
        } else {
            mercadoPagoCobroIntentRepository.save(intent);
            registrar(company, intent, MercadoPagoTrazaEtapaEnum.LIQUIDACION_PENDIENTE, MercadoPagoTrazaNivelEnum.INFO,
                    externalReference, intent.getPreferenceId(), paymentId,
                    "Pago sin liquidar | status=" + paymentStatus);
        }
    }

    private String mensajeLiquidacionAprobada(MercadoPagoCobroTipoEnum tipo, Long paymentId, int itemsCount) {
        if (tipo == MercadoPagoCobroTipoEnum.CUOTA) {
            return "Cuotas liquidadas | cuotas=" + itemsCount;
        }
        if (tipo == MercadoPagoCobroTipoEnum.ACCESO_CLUB) {
            return "Acceso invitado liquidado | paymentId=" + paymentId;
        }
        if (tipo == MercadoPagoCobroTipoEnum.RESERVA_AMENITY) {
            return "Reserva amenity liquidada | paymentId=" + paymentId;
        }
        return "Liquidación OTRO | paymentId=" + paymentId;
    }

    private void registrar(
            Company company,
            MercadoPagoCobroIntent intent,
            MercadoPagoTrazaEtapaEnum etapa,
            MercadoPagoTrazaNivelEnum nivel,
            String externalReference,
            String preferenceId,
            Long paymentId,
            String mensaje) {
        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .intent(intent)
                .etapa(etapa)
                .nivel(nivel)
                .externalReference(externalReference)
                .preferenceId(preferenceId)
                .paymentId(paymentId)
                .mensaje(mensaje)
                .build());
    }

    private boolean montoCoincide(BigDecimal esperado, BigDecimal recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return esperado.compareTo(recibido) == 0;
    }
}
