package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Repository.MercadoPagoCobroIntentRepository;
import com.blue.open.Code.Repository.MercadoPagoCobroRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoCobroIntentService {

    private final MercadoPagoCobroIntentRepository mercadoPagoCobroIntentRepository;
    private final MercadoPagoCobroRepository mercadoPagoCobroRepository;
    private final EntityManager entityManager;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;
    private final TransactionTemplate transactionTemplate;

    public MercadoPagoCobroIntent crear(
            Company company,
            String externalReference,
            String preferenceId,
            List<Cuota> cuotas,
            User registradoPor) {

        CreacionResult creacion = transactionTemplate.execute(status -> {
            MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
            intent.setCompany(company);
            intent.setExternalReference(externalReference);
            intent.setPreferenceId(preferenceId);
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.PENDIENTE);
            MercadoPagoCobroIntent savedIntent = mercadoPagoCobroIntentRepository.save(intent);

            MercadoPagoCobro mercadoPagoCobro = new MercadoPagoCobro();
            mercadoPagoCobro.setCompany(company);
            mercadoPagoCobro.setRegistradoPor(registradoPor);
            mercadoPagoCobro.setTipo(MercadoPagoCobroTipoEnum.CUOTA);
            mercadoPagoCobro.setMontoTotal(calcularMontoTotal(cuotas));
            mercadoPagoCobro.setMercadoPagoCobroIntent(savedIntent);
            for (Cuota cuota : cuotas) {
                mercadoPagoCobro.addItem(
                        entityManager.getReference(Cuota.class, cuota.getId()),
                        cuota.montoFinal(),
                        cuota.getConcepto());
            }
            MercadoPagoCobro savedCobro = mercadoPagoCobroRepository.save(mercadoPagoCobro);

            return new CreacionResult(savedIntent, savedCobro);
        });

        MercadoPagoCobroIntent savedIntent = creacion.intent();
        MercadoPagoCobro savedCobro = creacion.cobro();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .intent(savedIntent)
                .etapa(MercadoPagoTrazaEtapaEnum.INTENT_CREADO)
                .externalReference(externalReference)
                .preferenceId(preferenceId)
                .responseRaw(rawJsonSerializer.toJson(Map.of(
                        "intentUuid", savedIntent.getUuid(),
                        "mercadoPagoCobroUuid", savedCobro.getUuid(),
                        "cuotas", cuotas.size(),
                        "montoTotal", savedCobro.getMontoTotal())))
                .mensaje("Intent MP creado | cuotas=" + cuotas.size())
                .build());

        return savedIntent;
    }

    public MercadoPagoCobroIntent crearParaAccesoClub(
            Company company,
            String externalReference,
            String preferenceId,
            EventoAcceso evento,
            String concepto,
            BigDecimal montoTotal,
            BigDecimal precioUnitario,
            int cantidadEntradas,
            User registradoPor,
            EventoAccesoHistorialTipoEnum tipo,
            Member member,
            String nombre) {

        CreacionResult creacion = transactionTemplate.execute(status -> {
            MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
            intent.setCompany(company);
            intent.setExternalReference(externalReference);
            intent.setPreferenceId(preferenceId);
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.PENDIENTE);
            MercadoPagoCobroIntent savedIntent = mercadoPagoCobroIntentRepository.save(intent);

            MercadoPagoCobro mercadoPagoCobro = new MercadoPagoCobro();
            mercadoPagoCobro.setCompany(company);
            mercadoPagoCobro.setRegistradoPor(registradoPor);
            mercadoPagoCobro.setTipo(MercadoPagoCobroTipoEnum.ACCESO_CLUB);
            mercadoPagoCobro.setMontoTotal(montoTotal);
            mercadoPagoCobro.setTitulo(concepto);
            mercadoPagoCobro.setDetalle(evento.getDescripcion());
            mercadoPagoCobro.setEventoAcceso(evento);
            mercadoPagoCobro.setMercadoPagoCobroIntent(savedIntent);
            String itemDetalle = cantidadEntradas > 1
                    ? cantidadEntradas + " x " + precioUnitario
                    : evento.getDescripcion();
            mercadoPagoCobro.addItemSinCuota(montoTotal, concepto, itemDetalle);
            mercadoPagoCobro.asignarEventoAccesoDatos(tipo, member, nombre);
            MercadoPagoCobro savedCobro = mercadoPagoCobroRepository.save(mercadoPagoCobro);

            return new CreacionResult(savedIntent, savedCobro);
        });

        MercadoPagoCobroIntent savedIntent = creacion.intent();
        MercadoPagoCobro savedCobro = creacion.cobro();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .intent(savedIntent)
                .etapa(MercadoPagoTrazaEtapaEnum.INTENT_CREADO)
                .externalReference(externalReference)
                .preferenceId(preferenceId)
                .responseRaw(rawJsonSerializer.toJson(Map.of(
                        "intentUuid", savedIntent.getUuid(),
                        "mercadoPagoCobroUuid", savedCobro.getUuid(),
                        "eventoAccesoUuid", evento.getUuid(),
                        "montoTotal", savedCobro.getMontoTotal())))
                .mensaje("Intent MP creado | acceso_club eventoUuid=" + evento.getUuid())
                .build());

        return savedIntent;
    }

    public MercadoPagoCobroIntent crearParaReservaAmenity(
            Company company,
            String externalReference,
            String preferenceId,
            Reserva reserva,
            ReservaAmenityCobroTipoEnum tipoCobro,
            BigDecimal montoTotal,
            String concepto,
            User registradoPor) {

        CreacionResult creacion = transactionTemplate.execute(status -> {
            MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
            intent.setCompany(company);
            intent.setExternalReference(externalReference);
            intent.setPreferenceId(preferenceId);
            intent.setEstado(MercadoPagoCobroIntentEstadoEnum.PENDIENTE);
            MercadoPagoCobroIntent savedIntent = mercadoPagoCobroIntentRepository.save(intent);

            MercadoPagoCobro mercadoPagoCobro = new MercadoPagoCobro();
            mercadoPagoCobro.setCompany(company);
            mercadoPagoCobro.setRegistradoPor(registradoPor);
            mercadoPagoCobro.setTipo(MercadoPagoCobroTipoEnum.RESERVA_AMENITY);
            mercadoPagoCobro.setMontoTotal(montoTotal);
            mercadoPagoCobro.setTitulo(concepto);
            mercadoPagoCobro.setReserva(entityManager.getReference(Reserva.class, reserva.getId()));
            mercadoPagoCobro.setReservaCobroTipo(tipoCobro);
            mercadoPagoCobro.setMercadoPagoCobroIntent(savedIntent);
            mercadoPagoCobro.addItemSinCuota(montoTotal, concepto, tipoCobro.name());
            MercadoPagoCobro savedCobro = mercadoPagoCobroRepository.save(mercadoPagoCobro);

            return new CreacionResult(savedIntent, savedCobro);
        });

        MercadoPagoCobroIntent savedIntent = creacion.intent();
        MercadoPagoCobro savedCobro = creacion.cobro();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .intent(savedIntent)
                .etapa(MercadoPagoTrazaEtapaEnum.INTENT_CREADO)
                .externalReference(externalReference)
                .preferenceId(preferenceId)
                .responseRaw(rawJsonSerializer.toJson(Map.of(
                        "intentUuid", savedIntent.getUuid(),
                        "mercadoPagoCobroUuid", savedCobro.getUuid(),
                        "reservaUuid", reserva.getUuid(),
                        "tipoCobro", tipoCobro.name(),
                        "montoTotal", savedCobro.getMontoTotal())))
                .mensaje("Intent MP creado | reserva_amenity reservaUuid=" + reserva.getUuid()
                        + " | tipo=" + tipoCobro)
                .build());

        return savedIntent;
    }

    private BigDecimal calcularMontoTotal(List<Cuota> cuotas) {
        return cuotas.stream()
                .map(Cuota::montoFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record CreacionResult(MercadoPagoCobroIntent intent, MercadoPagoCobro cobro) {
    }
}
