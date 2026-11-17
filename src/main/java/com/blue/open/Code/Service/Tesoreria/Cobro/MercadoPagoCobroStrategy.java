package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.CobroCuotaCommand;
import com.blue.open.Code.Entity.DTO.CobroCuotaItemResultDTO;
import com.blue.open.Code.Entity.DTO.CobroCuotaResultDTO;
import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MercadoPagoCobroStrategy implements CanalCobroStrategy {

    private final CuotaRepository cuotaRepository;
    private final MercadoPagoClientService mercadoPagoClientService;
    private final MercadoPagoPreferenceBuilder mercadoPagoPreferenceBuilder;
    private final MercadoPagoCobroIntentService mercadoPagoCobroIntentService;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;

    @Override
    public CanalCobroEnum getCanal() {
        return CanalCobroEnum.MERCADO_PAGO;
    }

    @Override
    public CobroCuotaResultDTO procesar(CobroCuotaCommand command, Company company, User registradoPor) {
        List<String> cuotaUuids = dedupe(command.getCuotaUuids());
        Map<String, Cuota> cuotasByUuid = loadCuotasByUuid(cuotaUuids, company.getId());

        CobroCuotaResultDTO result = new CobroCuotaResultDTO();
        List<Cuota> cuotasValidas = new ArrayList<>();

        for (String cuotaUuid : cuotaUuids) {
            CobroCuotaItemResultDTO item = validarCuota(cuotaUuid, cuotasByUuid);
            result.getItems().add(item);
            if (item.isExitoso()) {
                cuotasValidas.add(cuotasByUuid.get(cuotaUuid));
            } else {
                result.setFallidas(result.getFallidas() + 1);
            }
        }

        if (cuotasValidas.size() != cuotaUuids.size()) {
            return result;
        }

        String externalReference = UUID.randomUUID().toString();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .etapa(MercadoPagoTrazaEtapaEnum.COBRO_INICIADO)
                .externalReference(externalReference)
                .requestRaw(rawJsonSerializer.toJson(Map.of("cuotaUuids", cuotaUuids)))
                .mensaje("Cobro MP iniciado | cuotas=" + cuotasValidas.size())
                .build());

        PreferenceRequest preferenceRequest = mercadoPagoPreferenceBuilder.build(company, cuotasValidas, externalReference);
        Preference preference = mercadoPagoClientService.createPreference(company, preferenceRequest);

        mercadoPagoCobroIntentService.crear(
                company, externalReference, preference.getId(), cuotasValidas, registradoPor);

        result.setMercadoPagoPreferenceId(preference.getId());
        result.setMercadoPagoInitPoint(preference.getInitPoint());
        result.setMercadoPagoSandboxInitPoint(preference.getSandboxInitPoint());
        result.setExitosas(cuotasValidas.size());

        return result;
    }

    private CobroCuotaItemResultDTO validarCuota(String cuotaUuid, Map<String, Cuota> cuotasByUuid) {
        CobroCuotaItemResultDTO item = new CobroCuotaItemResultDTO();
        item.setCuotaUuid(cuotaUuid);

        Cuota cuota = cuotasByUuid.get(cuotaUuid);
        if (cuota == null) {
            item.setExitoso(false);
            item.setMensajeError("Cuota no encontrada");
            return item;
        }

        String validationError = validateEstado(cuota);
        if (validationError != null) {
            item.setExitoso(false);
            item.setMensajeError(validationError);
            return item;
        }

        item.setExitoso(true);
        return item;
    }

    private String validateEstado(Cuota cuota) {
        CuotaEstadoEnum estado = cuota.getEstado();
        if (estado == CuotaEstadoEnum.PAGADA) {
            return "Cuota ya pagada";
        }
        if (estado == CuotaEstadoEnum.ANULADA) {
            return "Cuota anulada";
        }
        if (estado != CuotaEstadoEnum.PENDIENTE && estado != CuotaEstadoEnum.VENCIDA) {
            return "Cuota no puede cobrarse en su estado actual";
        }
        return null;
    }

    private Map<String, Cuota> loadCuotasByUuid(List<String> cuotaUuids, Long companyId) {
        if (cuotaUuids.isEmpty()) {
            return Map.of();
        }
        return cuotaRepository.findByUuidInAndCompanyIdWithPersonas(cuotaUuids, companyId).stream()
                .collect(Collectors.toMap(Cuota::getUuid, Function.identity(), (a, b) -> a));
    }

    private List<String> dedupe(List<String> uuids) {
        if (uuids == null) {
            return List.of();
        }
        return uuids.stream()
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }
}
