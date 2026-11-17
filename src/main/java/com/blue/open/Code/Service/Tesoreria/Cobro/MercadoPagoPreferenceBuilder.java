package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Config.FrontUrlProperties;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoPreferenceBuilder {

    private static final String CURRENCY_ARS = "ARS";

    private final FrontUrlProperties frontUrlProperties;

    public PreferenceRequest build(Company company, List<Cuota> cuotas, String externalReference) {
        List<PreferenceItemRequest> items = cuotas.stream()
                .map(this::toItem)
                .toList();
        return buildRequest(company, items, externalReference);
    }

    public PreferenceRequest buildParaEventoAcceso(
            Company company,
            String eventoUuid,
            String concepto,
            BigDecimal precioUnitario,
            int cantidadEntradas,
            String externalReference) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(eventoUuid)
                .title(concepto)
                .quantity(cantidadEntradas)
                .unitPrice(precioUnitario)
                .currencyId(CURRENCY_ARS)
                .build();
        return buildRequest(company, List.of(item), externalReference);
    }

    public PreferenceRequest buildParaReservaAmenity(
            Company company,
            String reservaUuid,
            String concepto,
            BigDecimal monto,
            String externalReference) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(reservaUuid)
                .title(concepto)
                .quantity(1)
                .unitPrice(monto)
                .currencyId(CURRENCY_ARS)
                .build();
        return buildRequest(company, List.of(item), externalReference);
    }

    private PreferenceRequest buildRequest(
            Company company,
            List<PreferenceItemRequest> items,
            String externalReference) {
        String baseUrl = trimTrailingSlash(frontUrlProperties.getUrl());
        PreferenceBackUrlsRequest redirectUrls = PreferenceBackUrlsRequest.builder()
                .success(baseUrl + "/pago/exito")
                .failure(baseUrl + "/pago/fallo")
                .pending(baseUrl + "/pago/pendiente")
                .build();
        return PreferenceRequest.builder()
                .items(items)
                .externalReference(externalReference)
                .backUrls(redirectUrls)
                .notificationUrl(baseUrl + "/api/mercado-pago-webhook/" + company.getUuid())
                .build();
    }

    private PreferenceItemRequest toItem(Cuota cuota) {
        return PreferenceItemRequest.builder()
                .id(cuota.getUuid())
                .title(cuota.getConcepto())
                .quantity(1)
                .unitPrice(cuota.montoFinal())
                .currencyId(CURRENCY_ARS)
                .build();
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
