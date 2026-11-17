package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.MercadoPagoWebhookNotificationDTO;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("mercado-pago-webhook")
public class MercadoPagoWebhookController {

    @Autowired
    private MercadoPagoWebhookService mercadoPagoWebhookService;

    @PostMapping("{companyUuid}")
    public ResponseEntity<Void> recibir(
            @PathVariable String companyUuid,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "data_id", required = false) String dataIdUnderscore,
            @RequestParam(value = "type", required = false) String type,
            @RequestBody(required = false) MercadoPagoWebhookNotificationDTO body) {

        String resolvedDataId = dataId != null && !dataId.isBlank() ? dataId : dataIdUnderscore;
        mercadoPagoWebhookService.procesarNotificacion(
                companyUuid, xSignature, xRequestId, resolvedDataId, type, body);
        return ResponseEntity.ok().build();
    }
}
