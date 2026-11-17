package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.CompanyMercadoPagoRepository;
import com.blue.open.Code.Service.MercadoPagoCredentialsCryptoService;
import com.blue.open.Core.Exceptions.CustomException;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoClientService {

    private final PreferenceClient preferenceClient = new PreferenceClient();
    private final PaymentClient paymentClient = new PaymentClient();
    private final CompanyMercadoPagoRepository companyMercadoPagoRepository;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;

    @Autowired
    private MercadoPagoCredentialsCryptoService credentialsCryptoService;

    public Preference createPreference(Company company, PreferenceRequest request) {
        CompanyMercadoPago config = resolveOperativo(company);
        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(decryptAccessToken(config, company))
                .build();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .etapa(MercadoPagoTrazaEtapaEnum.PREFERENCIA_REQUEST)
                .requestRaw(rawJsonSerializer.toJson(request))
                .mensaje("Solicitud createPreference a Mercado Pago")
                .build());

        try {
            Preference preference = preferenceClient.create(request, options);
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PREFERENCIA_RESPONSE)
                    .preferenceId(preference.getId())
                    .externalReference(request.getExternalReference())
                    .responseRaw(rawJsonSerializer.toJson(preference))
                    .mensaje("Preferencia creada en Mercado Pago")
                    .build());
            return preference;
        } catch (MPApiException ex) {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PREFERENCIA_ERROR)
                    .nivel(MercadoPagoTrazaNivelEnum.ERROR)
                    .externalReference(request.getExternalReference())
                    .responseRaw(rawJsonSerializer.apiExceptionToJson(ex))
                    .mensaje("Error MPApiException al crear preferencia")
                    .build());
            throw new CustomException(HttpStatus.BAD_GATEWAY,
                    "Error al crear preferencia de pago en Mercado Pago",
                    "MercadoPagoClientService | MPApiException | status: " + ex.getStatusCode()
                            + " | " + ex.getMessage());
        } catch (MPException ex) {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PREFERENCIA_ERROR)
                    .nivel(MercadoPagoTrazaNivelEnum.ERROR)
                    .externalReference(request.getExternalReference())
                    .responseRaw(rawJsonSerializer.toJson(Map.of("message", ex.getMessage())))
                    .mensaje("Error MPException al crear preferencia")
                    .build());
            throw new CustomException(HttpStatus.BAD_GATEWAY,
                    "Error al comunicarse con Mercado Pago",
                    "MercadoPagoClientService | MPException | " + ex.getMessage());
        }
    }

    public Payment getPayment(Company company, Long paymentId) {
        CompanyMercadoPago config = resolveOperativo(company);
        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(decryptAccessToken(config, company))
                .build();

        try {
            Payment payment = paymentClient.get(paymentId, options);
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PAGO_CONSULTA_RESPONSE)
                    .paymentId(paymentId)
                    .externalReference(payment.getExternalReference())
                    .requestRaw(rawJsonSerializer.toJson(Map.of("paymentId", paymentId)))
                    .responseRaw(rawJsonSerializer.toJson(payment))
                    .mensaje("Pago consultado en Mercado Pago | status=" + payment.getStatus())
                    .build());
            return payment;
        } catch (MPApiException ex) {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PAGO_CONSULTA_ERROR)
                    .nivel(MercadoPagoTrazaNivelEnum.ERROR)
                    .paymentId(paymentId)
                    .requestRaw(rawJsonSerializer.toJson(Map.of("paymentId", paymentId)))
                    .responseRaw(rawJsonSerializer.apiExceptionToJson(ex))
                    .mensaje("Error MPApiException al consultar pago")
                    .build());
            throw new CustomException(HttpStatus.BAD_GATEWAY,
                    "Error al consultar pago en Mercado Pago",
                    "MercadoPagoClientService | MPApiException | status: " + ex.getStatusCode()
                            + " | paymentId: " + paymentId + " | " + ex.getMessage());
        } catch (MPException ex) {
            mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                    .company(company)
                    .etapa(MercadoPagoTrazaEtapaEnum.PAGO_CONSULTA_ERROR)
                    .nivel(MercadoPagoTrazaNivelEnum.ERROR)
                    .paymentId(paymentId)
                    .requestRaw(rawJsonSerializer.toJson(Map.of("paymentId", paymentId)))
                    .responseRaw(rawJsonSerializer.toJson(Map.of("message", ex.getMessage())))
                    .mensaje("Error MPException al consultar pago")
                    .build());
            throw new CustomException(HttpStatus.BAD_GATEWAY,
                    "Error al comunicarse con Mercado Pago",
                    "MercadoPagoClientService | MPException | paymentId: " + paymentId
                            + " | " + ex.getMessage());
        }
    }

    public CompanyMercadoPago resolveOperativo(Company company) {
        return companyMercadoPagoRepository.findActiveByCompanyId(company.getId())
                .filter(CompanyMercadoPago::isOperativo)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST,
                        "Mercado Pago no configurado para este gimnasio",
                        "MercadoPagoClientService | companyId: " + company.getId()));
    }

    String decryptAccessToken(CompanyMercadoPago config, Company company) {
        return credentialsCryptoService.decrypt(config.getAccessToken(), company.getUuid(),
                MercadoPagoCredentialsCryptoService.ACCESS_TOKEN_FIELD);
    }
}
