package com.blue.open.Code.Service.EventoAcceso.Cobro;

import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoSocioCobroCommand;
import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.MemberRepository;
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
import com.blue.open.Core.Repository.UserRepository;
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
public class EventoAccesoCobroSocioStrategy implements EventoAccesoCobroStrategy {

    private static final int STATUS_ACTIVE = 0;

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
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
        return EventoAccesoHistorialTipoEnum.SOCIO;
    }

    @Override
    public EventoAccesoCobroResultDTO cobrar(Company company, User registradoPor, EventoAcceso evento, Object payload) {
        EventoAccesoSocioCobroCommand command = parsePayload(payload);

        User user = requireUserInCompany(command.getUserUuid(), company.getId());
        Member member = requireMember(user, company.getId());

        String nombre = user.getNombre() != null ? user.getNombre() : "";
        String apellido = user.getApellido() != null ? user.getApellido() : "";
        String numeroSocio = member.getNumeroSocio() != null ? member.getNumeroSocio() : "-";
        String nombreSnapshot = (nombre + " " + apellido).trim();

        BigDecimal precioUnitario;
        String conceptoPrefix;

        switch (member.getEstado()) {
            case MOROSO -> {
                boolean permitido = Boolean.TRUE.equals(evento.getPermiteMorosos());
                boolean montoValido = evento.getMontoMorosos() != null
                        && evento.getMontoMorosos().compareTo(BigDecimal.ZERO) > 0;
                if (!permitido || !montoValido) {
                    String motivo = "%s %s (socio %s) es moroso y este evento no permite el acceso de morosos"
                            .formatted(nombre, apellido, numeroSocio);
                    eventoAccesoHistorialService.registrarSocio(
                            company, evento, member, nombreSnapshot, EventoAccesoScanEstadoEnum.BLOQUEADO, motivo, null);
                    throw new CustomException(HttpStatus.FORBIDDEN, motivo,
                            "EventoAccesoCobroSocioStrategy | memberUuid: " + member.getUuid());
                }
                precioUnitario = evento.getMontoMorosos();
                conceptoPrefix = "Acceso moroso";
            }
            case ACTIVO -> {
                boolean montoValido = evento.getMonto() != null && evento.getMonto().compareTo(BigDecimal.ZERO) > 0;
                if (!montoValido) {
                    throw new CustomException(HttpStatus.CONFLICT, "El socio no está moroso",
                            "EventoAccesoCobroSocioStrategy | memberUuid: " + member.getUuid());
                }
                precioUnitario = evento.getMonto();
                conceptoPrefix = "Acceso socio";
            }
            case INHABILITADO, SUSPENDIDO, INACTIVO -> throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "Acceso no permitido para socios en estado " + formatEstado(member.getEstado()),
                    "EventoAccesoCobroSocioStrategy | memberId: " + member.getId());
            default -> throw new IllegalStateException("Estado de member no soportado: " + member.getEstado());
        }

        int cantidadEntradas = EventoAccesoCobroSupport.resolveCantidadEntradas(command.getCantidadEntradas());
        BigDecimal montoTotal = EventoAccesoCobroSupport.calcularMontoTotal(precioUnitario, cantidadEntradas);
        String concepto = conceptoPrefix + " - " + evento.getTitulo() + " - " + nombre + " " + apellido
                + " (socio " + numeroSocio + ")" + EventoAccesoCobroSupport.sufijoCantidadEntradas(cantidadEntradas);

        EventoAccesoCobroResultDTO result = switch (command.getCanalCobro()) {
            case MANUAL -> cobrarManual(command, company, evento, member, cantidadEntradas, montoTotal, concepto, registradoPor);
            case MERCADO_PAGO -> cobrarMercadoPago(company, evento, member, nombreSnapshot, precioUnitario, cantidadEntradas, montoTotal, concepto, registradoPor);
        };

        EventoAccesoScanEstadoEnum resultado = command.getCanalCobro() == CanalCobroEnum.MANUAL
                ? EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO
                : EventoAccesoScanEstadoEnum.DEBE_PAGAR;

        result.setTipo(EventoAccesoHistorialTipoEnum.SOCIO);
        result.setResultado(resultado);
        result.setMemberUuid(member.getUuid());
        result.setNombre(nombre);
        result.setApellido(apellido);
        result.setNumeroSocio(numeroSocio);

        eventoAccesoHistorialService.registrarSocio(company, evento, member, nombreSnapshot, resultado, concepto, montoTotal);

        return result;
    }

    private EventoAccesoCobroResultDTO cobrarManual(
            EventoAccesoSocioCobroCommand command,
            Company company,
            EventoAcceso evento,
            Member member,
            int cantidadEntradas,
            BigDecimal montoTotal,
            String concepto,
            User registradoPor) {

        Member titular = member.isTitular() ? member : member.getTitular();

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(company);
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(montoTotal);
        movimiento.setFecha(command.getFecha() != null ? command.getFecha() : LocalDate.now());
        movimiento.setMedioPago(command.getMedioPago());
        movimiento.setEventoAcceso(evento);
        movimiento.setPersona(member);
        movimiento.setTitular(titular);
        movimiento.setConcepto(concepto);
        movimiento.setRegistradoPor(registradoPor);
        movimiento.setObservaciones(command.getObservaciones());

        movimiento = movimientoRepository.save(movimiento);
        return toResult(evento, cantidadEntradas, montoTotal, concepto, movimiento.getUuid(), null);
    }

    private EventoAccesoCobroResultDTO cobrarMercadoPago(
            Company company,
            EventoAcceso evento,
            Member member,
            String nombreSnapshot,
            BigDecimal precioUnitario,
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
                        "memberUuid", member.getUuid(),
                        "cantidadEntradas", cantidadEntradas,
                        "tipo", "ACCESO_CLUB_MOROSO")))
                .mensaje("Cobro MP moroso evento acceso | eventoUuid=" + evento.getUuid()
                        + " | memberUuid=" + member.getUuid() + " | entradas=" + cantidadEntradas)
                .build());

        PreferenceRequest preferenceRequest = mercadoPagoPreferenceBuilder.buildParaEventoAcceso(
                company, evento.getUuid(), concepto, precioUnitario, cantidadEntradas, externalReference);
        Preference preference = mercadoPagoClientService.createPreference(company, preferenceRequest);

        if (preference.getInitPoint() == null || preference.getInitPoint().isBlank()) {
            throw new CustomException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió URL de pago",
                    "EventoAccesoCobroSocioStrategy | initPoint vacío");
        }

        mercadoPagoCobroIntentService.crearParaAccesoClub(
                company, externalReference, preference.getId(), evento, concepto,
                montoTotal, precioUnitario, cantidadEntradas, registradoPor,
                EventoAccesoHistorialTipoEnum.SOCIO, member, nombreSnapshot);

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
        dto.setCantidadEntradas(cantidadEntradas);
        dto.setMonto(montoTotal);
        dto.setConcepto(concepto);
        dto.setMovimientoUuid(movimientoUuid);
        dto.setQr(qr);
        return dto;
    }

    private Member requireMember(User user, Long companyId) {
        return memberRepository.findByUserIdAndCompanyIdWithDetails(user.getId(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "EventoAccesoCobroSocioStrategy | userId: " + user.getId()));
    }

    private User requireUserInCompany(String userUuid, Long companyId) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Usuario requerido",
                    "EventoAccesoCobroSocioStrategy | userUuid vacío");
        }

        User user = userRepository.findFirstByUuidWithDetails(userUuid.trim());
        if (user == null || user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado",
                    "EventoAccesoCobroSocioStrategy | userUuid: " + userUuid);
        }

        if (user.getUserCompanyRoles() == null || user.getUserCompanyRoles().stream()
                .noneMatch(ucr -> ucr.getCompany() != null && companyId.equals(ucr.getCompany().getId()))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El usuario no pertenece a este gimnasio",
                    "EventoAccesoCobroSocioStrategy | userUuid: " + userUuid);
        }

        return user;
    }

    private String formatEstado(MemberEstadoEnum estado) {
        return estado.name().toLowerCase().replace('_', ' ');
    }

    private EventoAccesoSocioCobroCommand parsePayload(Object payload) {
        EventoAccesoSocioCobroCommand command;
        try {
            command = objectMapper.convertValue(payload, EventoAccesoSocioCobroCommand.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Payload inválido para cobro de socio",
                    "EventoAccesoCobroSocioStrategy | " + e.getMessage());
        }

        Set<ConstraintViolation<EventoAccesoSocioCobroCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            String mensaje = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new CustomException(HttpStatus.BAD_REQUEST, mensaje,
                    "EventoAccesoCobroSocioStrategy | payload inválido");
        }
        return command;
    }
}
