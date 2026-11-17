package com.blue.open.Code.Service.EventoAcceso.Cobro;

import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoInvitadoCommand;
import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Service.EventoAcceso.EventoAccesoCobroSupport;
import com.blue.open.Code.Service.EventoAcceso.EventoAccesoHistorialService;
import com.blue.open.Code.Service.QrCodeService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoClientService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoCobroIntentService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoPreferenceBuilder;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoRawJsonSerializer;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoTrazaService;
import com.blue.open.Core.Exceptions.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventoAccesoCobroInvitadoStrategy implements EventoAccesoCobroStrategy {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final MovimientoRepository movimientoRepository;
    private final MercadoPagoClientService mercadoPagoClientService;
    private final MercadoPagoPreferenceBuilder mercadoPagoPreferenceBuilder;
    private final MercadoPagoCobroIntentService mercadoPagoCobroIntentService;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;
    private final QrCodeService qrCodeService;
    private final EventoAccesoHistorialService eventoAccesoHistorialService;

    @Override
    public EventoAccesoHistorialTipoEnum getTipo() {
        return EventoAccesoHistorialTipoEnum.INVITADO;
    }

    @Override
    public EventoAccesoCobroResultDTO cobrar(Company company, User registradoPor, EventoAcceso evento, Object payload) {
        EventoAccesoInvitadoCommand command = parsePayload(payload);

        boolean permitido = Boolean.TRUE.equals(evento.getPermiteInvitados());
        boolean montoValido = evento.getMontoInvitados() != null
                && evento.getMontoInvitados().compareTo(BigDecimal.ZERO) > 0;
        if (!permitido || !montoValido) {
            String motivo = "El evento \"" + evento.getTitulo() + "\" no permite el acceso de invitados";
            eventoAccesoHistorialService.registrarInvitado(
                    company, evento, command.getNombre(), EventoAccesoScanEstadoEnum.BLOQUEADO, motivo, null);
            throw new CustomException(HttpStatus.FORBIDDEN, motivo,
                    "EventoAccesoCobroInvitadoStrategy | eventoUuid: " + evento.getUuid());
        }

        int cantidadEntradas = EventoAccesoCobroSupport.resolveCantidadEntradas(command.getCantidadEntradas());
        BigDecimal montoTotal = EventoAccesoCobroSupport.calcularMontoTotal(evento.getMontoInvitados(), cantidadEntradas);
        String concepto = "Acceso invitado - " + evento.getTitulo()
                + EventoAccesoCobroSupport.sufijoCantidadEntradas(cantidadEntradas);

        EventoAccesoCobroResultDTO result = switch (command.getCanalCobro()) {
            case MANUAL -> cobrarManual(command, company, evento, cantidadEntradas, montoTotal, concepto, registradoPor);
            case MERCADO_PAGO -> cobrarMercadoPago(command, company, evento, cantidadEntradas, montoTotal, concepto, registradoPor);
        };

        EventoAccesoScanEstadoEnum resultado = command.getCanalCobro() == CanalCobroEnum.MANUAL
                ? EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO
                : EventoAccesoScanEstadoEnum.DEBE_PAGAR;
        result.setResultado(resultado);
        eventoAccesoHistorialService.registrarInvitado(
                company, evento, command.getNombre(), resultado, concepto, montoTotal);

        return result;
    }

    private EventoAccesoCobroResultDTO cobrarManual(
            EventoAccesoInvitadoCommand command,
            Company company,
            EventoAcceso evento,
            int cantidadEntradas,
            BigDecimal montoTotal,
            String concepto,
            User registradoPor) {

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(company);
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(montoTotal);
        movimiento.setFecha(command.getFecha() != null ? command.getFecha() : LocalDate.now());
        movimiento.setMedioPago(command.getMedioPago());
        movimiento.setEventoAcceso(evento);
        movimiento.setConcepto(concepto);
        movimiento.setRegistradoPor(registradoPor);
        movimiento.setObservaciones(command.getObservaciones());

        movimiento = movimientoRepository.save(movimiento);
        return toResult(evento, cantidadEntradas, montoTotal, concepto, movimiento.getUuid(), null);
    }

    private EventoAccesoCobroResultDTO cobrarMercadoPago(
            EventoAccesoInvitadoCommand command,
            Company company,
            EventoAcceso evento,
            int cantidadEntradas,
            BigDecimal montoTotal,
            String concepto,
            User registradoPor) {

        mercadoPagoClientService.resolveOperativo(company);

        String externalReference = UUID.randomUUID().toString();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .etapa(com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum.COBRO_INICIADO)
                .externalReference(externalReference)
                .requestRaw(rawJsonSerializer.toJson(Map.of(
                        "eventoAccesoUuid", evento.getUuid(),
                        "cantidadEntradas", cantidadEntradas,
                        "tipo", "ACCESO_CLUB")))
                .mensaje("Cobro MP invitado evento acceso | eventoUuid=" + evento.getUuid()
                        + " | entradas=" + cantidadEntradas)
                .build());

        PreferenceRequest preferenceRequest = mercadoPagoPreferenceBuilder.buildParaEventoAcceso(
                company, evento.getUuid(), concepto, evento.getMontoInvitados(), cantidadEntradas, externalReference);
        Preference preference = mercadoPagoClientService.createPreference(company, preferenceRequest);

        if (preference.getInitPoint() == null || preference.getInitPoint().isBlank()) {
            throw new CustomException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió URL de pago",
                    "EventoAccesoCobroInvitadoStrategy | initPoint vacío");
        }

        mercadoPagoCobroIntentService.crearParaAccesoClub(
                company, externalReference, preference.getId(), evento, concepto,
                montoTotal, evento.getMontoInvitados(), cantidadEntradas, registradoPor,
                EventoAccesoHistorialTipoEnum.INVITADO, null, command.getNombre());

        String qr = qrCodeService.generateBase64Png(preference.getInitPoint());
        return toResult(evento, cantidadEntradas, montoTotal, concepto, null, qr);
    }

    private EventoAccesoCobroResultDTO toResult(
            EventoAcceso evento,
            int cantidadEntradas,
            BigDecimal montoTotal,
            String concepto,
            String movimientoUuid,
            String qr) {
        EventoAccesoCobroResultDTO dto = new EventoAccesoCobroResultDTO();
        dto.setEventoUuid(evento.getUuid());
        dto.setTipo(EventoAccesoHistorialTipoEnum.INVITADO);
        dto.setCantidadEntradas(cantidadEntradas);
        dto.setMonto(montoTotal);
        dto.setConcepto(concepto);
        dto.setMovimientoUuid(movimientoUuid);
        dto.setQr(qr);
        return dto;
    }

    private EventoAccesoInvitadoCommand parsePayload(Object payload) {
        EventoAccesoInvitadoCommand command;
        try {
            command = objectMapper.convertValue(payload, EventoAccesoInvitadoCommand.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Payload inválido para cobro de invitado",
                    "EventoAccesoCobroInvitadoStrategy | " + e.getMessage());
        }

        Set<ConstraintViolation<EventoAccesoInvitadoCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            String mensaje = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new CustomException(HttpStatus.BAD_REQUEST, mensaje,
                    "EventoAccesoCobroInvitadoStrategy | payload inválido");
        }
        return command;
    }
}
