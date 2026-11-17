package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.QrCodeService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoClientService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoCobroIntentService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoPreferenceBuilder;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoRawJsonSerializer;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoTrazaService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaAmenityCobroService {

    private final AuthService authService;
    private final ReservaRepository reservaRepository;
    private final MovimientoRepository movimientoRepository;
    private final SlotRepository slotRepository;
    private final MercadoPagoClientService mercadoPagoClientService;
    private final MercadoPagoPreferenceBuilder mercadoPagoPreferenceBuilder;
    private final MercadoPagoCobroIntentService mercadoPagoCobroIntentService;
    private final MercadoPagoTrazaService mercadoPagoTrazaService;
    private final MercadoPagoRawJsonSerializer rawJsonSerializer;
    private final QrCodeService qrCodeService;

    @Transactional
    public ReservaAmenityCobroResultDTO registrarSena(String reservaUuid, ReservaAmenityCobroCommand command) {
        User registradoPor = authService.getActualOrThrow();
        Company company = authService.getCurrentCompanyOrThrow();

        Reserva reserva = loadReserva(reservaUuid, company.getId());
        validarNoGratuita(reserva, "confirmar");
        validarEstadoPendienteSena(reserva);

        if (reserva.getSenaPagadaAt() != null) {
            throw new CustomException(HttpStatus.CONFLICT, "La seña ya fue registrada",
                    "ReservaAmenityCobroService | reservaUuid: " + reservaUuid);
        }

        BigDecimal montoEsperado = reserva.getMontoSena();
        String concepto = ReservaAmenityCobroSupport.concepto(reserva, ReservaAmenityCobroTipoEnum.SENA);

        return switch (command.getCanalCobro()) {
            case MANUAL -> cobrarSenaManual(reserva, command, registradoPor, company, montoEsperado, concepto);
            case MERCADO_PAGO -> cobrarSenaMercadoPago(
                    reserva, company, montoEsperado, concepto, registradoPor);
        };
    }

    @Transactional
    public ReservaAmenityCobroResultDTO registrarSaldo(String reservaUuid, ReservaAmenityCobroCommand command) {
        User registradoPor = authService.getActualOrThrow();
        Company company = authService.getCurrentCompanyOrThrow();

        Reserva reserva = loadReserva(reservaUuid, company.getId());
        validarNoGratuita(reserva, "confirmar");

        if (reserva.getEstado() != ReservaEstadoEnum.CONFIRMADA) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Solo se puede cobrar saldo de reservas confirmadas",
                    "ReservaAmenityCobroService | estado: " + reserva.getEstado());
        }
        if (reserva.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(HttpStatus.CONFLICT, "No hay saldo pendiente",
                    "ReservaAmenityCobroService | reservaUuid: " + reservaUuid);
        }

        BigDecimal montoEsperado = reserva.getSaldoPendiente();
        String concepto = ReservaAmenityCobroSupport.concepto(reserva, ReservaAmenityCobroTipoEnum.SALDO);

        return switch (command.getCanalCobro()) {
            case MANUAL -> cobrarSaldoManual(reserva, command, registradoPor, company, montoEsperado, concepto);
            case MERCADO_PAGO -> cobrarSaldoMercadoPago(
                    reserva, company, montoEsperado, concepto, registradoPor);
        };
    }

    @Transactional
    public ReservaAmenityCobroResultDTO registrarTotal(String reservaUuid, ReservaAmenityCobroCommand command) {
        User registradoPor = authService.getActualOrThrow();
        Company company = authService.getCurrentCompanyOrThrow();

        Reserva reserva = loadReserva(reservaUuid, company.getId());
        validarNoGratuita(reserva, "confirmar");
        validarEstadoPendienteSena(reserva);

        if (reserva.getSenaPagadaAt() != null) {
            throw new CustomException(HttpStatus.CONFLICT, "La reserva ya fue cobrada",
                    "ReservaAmenityCobroService | reservaUuid: " + reservaUuid);
        }

        BigDecimal montoEsperado = reserva.getMontoTotal();
        String concepto = ReservaAmenityCobroSupport.concepto(reserva, ReservaAmenityCobroTipoEnum.TOTAL);

        return switch (command.getCanalCobro()) {
            case MANUAL -> cobrarTotalManual(reserva, command, registradoPor, company, montoEsperado, concepto);
            case MERCADO_PAGO -> cobrarTotalMercadoPago(
                    reserva, company, montoEsperado, concepto, registradoPor);
        };
    }

    private ReservaAmenityCobroResultDTO cobrarSenaManual(
            Reserva reserva,
            ReservaAmenityCobroCommand command,
            User registradoPor,
            Company company,
            BigDecimal montoEsperado,
            String concepto) {

        validarMontoEsperado(command.getMonto(), montoEsperado, "seña");

        Movimiento movimiento = buildMovimiento(reserva, command, registradoPor, company, concepto);
        movimientoRepository.save(movimiento);

        reserva.setSenaPagadaAt(new Timestamp(System.currentTimeMillis()));
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(
                ReservaAmenityMontoCalculator.saldoTrasSena(reserva.getMontoTotal(), reserva.getMontoSena()));
        reservaRepository.save(reserva);

        Slot slot = reserva.getSlot();
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slotRepository.save(slot);

        return toResult(reserva, ReservaAmenityCobroTipoEnum.SENA, montoEsperado, concepto, movimiento.getUuid(), null);
    }

    private ReservaAmenityCobroResultDTO cobrarSaldoManual(
            Reserva reserva,
            ReservaAmenityCobroCommand command,
            User registradoPor,
            Company company,
            BigDecimal montoEsperado,
            String concepto) {

        validarMontoEsperado(command.getMonto(), montoEsperado, "saldo");

        Movimiento movimiento = buildMovimiento(reserva, command, registradoPor, company, concepto);
        movimientoRepository.save(movimiento);

        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reservaRepository.save(reserva);

        return toResult(reserva, ReservaAmenityCobroTipoEnum.SALDO, montoEsperado, concepto, movimiento.getUuid(), null);
    }

    private ReservaAmenityCobroResultDTO cobrarTotalManual(
            Reserva reserva,
            ReservaAmenityCobroCommand command,
            User registradoPor,
            Company company,
            BigDecimal montoEsperado,
            String concepto) {

        validarMontoEsperado(command.getMonto(), montoEsperado, "total");

        Movimiento movimiento = buildMovimiento(reserva, command, registradoPor, company, concepto);
        movimientoRepository.save(movimiento);

        reserva.setSenaPagadaAt(new Timestamp(System.currentTimeMillis()));
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reservaRepository.save(reserva);

        Slot slot = reserva.getSlot();
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slotRepository.save(slot);

        return toResult(reserva, ReservaAmenityCobroTipoEnum.TOTAL, montoEsperado, concepto, movimiento.getUuid(), null);
    }

    private ReservaAmenityCobroResultDTO cobrarSenaMercadoPago(
            Reserva reserva,
            Company company,
            BigDecimal monto,
            String concepto,
            User registradoPor) {

        return iniciarMercadoPago(
                reserva, company, monto, concepto, registradoPor, ReservaAmenityCobroTipoEnum.SENA);
    }

    private ReservaAmenityCobroResultDTO cobrarSaldoMercadoPago(
            Reserva reserva,
            Company company,
            BigDecimal monto,
            String concepto,
            User registradoPor) {

        return iniciarMercadoPago(
                reserva, company, monto, concepto, registradoPor, ReservaAmenityCobroTipoEnum.SALDO);
    }

    private ReservaAmenityCobroResultDTO cobrarTotalMercadoPago(
            Reserva reserva,
            Company company,
            BigDecimal monto,
            String concepto,
            User registradoPor) {

        return iniciarMercadoPago(
                reserva, company, monto, concepto, registradoPor, ReservaAmenityCobroTipoEnum.TOTAL);
    }

    private ReservaAmenityCobroResultDTO iniciarMercadoPago(
            Reserva reserva,
            Company company,
            BigDecimal monto,
            String concepto,
            User registradoPor,
            ReservaAmenityCobroTipoEnum tipoCobro) {

        mercadoPagoClientService.resolveOperativo(company);

        String externalReference = UUID.randomUUID().toString();

        mercadoPagoTrazaService.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .etapa(MercadoPagoTrazaEtapaEnum.COBRO_INICIADO)
                .externalReference(externalReference)
                .requestRaw(rawJsonSerializer.toJson(Map.of(
                        "reservaUuid", reserva.getUuid(),
                        "tipoCobro", tipoCobro.name(),
                        "tipo", "RESERVA_AMENITY")))
                .mensaje("Cobro MP reserva amenity | reservaUuid=" + reserva.getUuid()
                        + " | tipo=" + tipoCobro)
                .build());

        PreferenceRequest preferenceRequest = mercadoPagoPreferenceBuilder.buildParaReservaAmenity(
                company, reserva.getUuid(), concepto, monto, externalReference);
        Preference preference = mercadoPagoClientService.createPreference(company, preferenceRequest);

        if (preference.getInitPoint() == null || preference.getInitPoint().isBlank()) {
            throw new CustomException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió URL de pago",
                    "ReservaAmenityCobroService | initPoint vacío");
        }

        mercadoPagoCobroIntentService.crearParaReservaAmenity(
                company, externalReference, preference.getId(), reserva, tipoCobro,
                monto, concepto, registradoPor);

        String qr = qrCodeService.generateBase64Png(preference.getInitPoint());
        return toResult(reserva, tipoCobro, monto, concepto, null, qr);
    }

    private ReservaAmenityCobroResultDTO toResult(
            Reserva reserva,
            ReservaAmenityCobroTipoEnum tipoCobro,
            BigDecimal monto,
            String concepto,
            String movimientoUuid,
            String qr) {
        ReservaAmenityCobroResultDTO dto = new ReservaAmenityCobroResultDTO();
        dto.setReservaUuid(reserva.getUuid());
        dto.setTipoCobro(tipoCobro);
        dto.setMonto(monto);
        dto.setConcepto(concepto);
        dto.setMovimientoUuid(movimientoUuid);
        dto.setQr(qr);
        return dto;
    }

    private Reserva loadReserva(String reservaUuid, Long companyId) {
        return reservaRepository.findByUuidAndCompanyIdWithDetails(reservaUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Reserva no encontrada",
                        "ReservaAmenityCobroService | uuid: " + reservaUuid));
    }

    private void validarNoGratuita(Reserva reserva, String accionAlternativa) {
        if (reserva.getMontoTotal().compareTo(BigDecimal.ZERO) == 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Reserva gratuita: use /" + accionAlternativa,
                    "ReservaAmenityCobroService | reservaUuid: " + reserva.getUuid());
        }
    }

    private void validarEstadoPendienteSena(Reserva reserva) {
        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La reserva no está pendiente de seña",
                    "ReservaAmenityCobroService | estado: " + reserva.getEstado());
        }
    }

    private void validarMontoEsperado(BigDecimal montoRecibido, BigDecimal montoEsperado, String tipo) {
        if (montoRecibido == null || montoEsperado == null
                || montoRecibido.compareTo(montoEsperado) != 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El monto no coincide con el " + tipo + " esperado",
                    "ReservaAmenityCobroService | recibido: " + montoRecibido + " esperado: " + montoEsperado);
        }
    }

    private Movimiento buildMovimiento(
            Reserva reserva,
            ReservaAmenityCobroCommand command,
            User registradoPor,
            Company company,
            String concepto) {

        Member titular = reserva.getMember();
        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(company);
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(command.getMonto());
        movimiento.setFecha(command.getFecha() != null ? command.getFecha() : LocalDate.now());
        movimiento.setMedioPago(command.getMedioPago());
        movimiento.setReserva(reserva);
        movimiento.setPersona(titular);
        movimiento.setTitular(titular);
        movimiento.setConcepto(concepto);
        movimiento.setRegistradoPor(registradoPor);
        movimiento.setObservaciones(command.getObservaciones());
        return movimiento;
    }
}
