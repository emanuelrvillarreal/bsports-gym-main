package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Core.Exceptions.CustomException;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoWebhookSignatureValidator {

    public void validate(String secret, String xSignature, String xRequestId, String dataId) {
        if (secret == null || secret.isBlank()) {
            throw invalid("Webhook de Mercado Pago no configurado para este club");
        }
        try {
            WebhookSignatureValidator.validate(xSignature, xRequestId, dataId, secret);
        } catch (MPInvalidWebhookSignatureException ex) {
            throw invalid("Firma de webhook de Mercado Pago inválida",
                    "MercadoPagoWebhookSignatureValidator | reason=" + ex.getReason()
                            + " | requestId=" + ex.getRequestId()
                            + " | timestamp=" + ex.getTimestamp());
        }
    }

    private CustomException invalid(String userMessage, String developerMessage) {
        return new CustomException(HttpStatus.UNAUTHORIZED, userMessage, developerMessage);
    }

    private CustomException invalid(String userMessage) {
        return invalid(userMessage, "MercadoPagoWebhookSignatureValidator | firma inválida");
    }
}
