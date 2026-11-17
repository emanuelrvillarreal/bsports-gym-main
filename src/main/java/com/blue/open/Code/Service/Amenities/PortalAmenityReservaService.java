package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.PortalReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.PortalReservaAmenityPagoCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortalAmenityReservaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private ReservaAmenityAltaService reservaAmenityAltaService;

    @Autowired
    private ReservaAmenityCobroService reservaAmenityCobroService;

    @Autowired
    private ReservaAmenityConfirmacionService reservaAmenityConfirmacionService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Transactional
    public ReservaAmenityCobroResultDTO reservar(String amenityUuid, PortalReservaAmenityAltaCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_AMENITIES_RESERVE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(memberAuthService.getCurrentMemberOrThrow(company.getId()));
        memberAuthService.requirePortalEligible(titular);

        ReservaAmenityAltaCommand altaCommand = new ReservaAmenityAltaCommand();
        altaCommand.setAmenityUuid(amenityUuid);
        altaCommand.setMemberUuid(titular.getUuid());
        altaCommand.setInicio(command.getFechaDesde());
        altaCommand.setFin(command.getFechaHasta());

        ReservaAmenityDTO reserva = reservaAmenityAltaService.crear(altaCommand);

        if (reserva.getMontoTotal().compareTo(BigDecimal.ZERO) == 0) {
            reservaAmenityConfirmacionService.confirmar(reserva.getReservaUuid());

            ReservaAmenityCobroResultDTO result = new ReservaAmenityCobroResultDTO();
            result.setReservaUuid(reserva.getReservaUuid());
            result.setMonto(BigDecimal.ZERO);
            return result;
        }

        ReservaAmenityCobroCommand cobroCommand = new ReservaAmenityCobroCommand();
        cobroCommand.setCanalCobro(CanalCobroEnum.MERCADO_PAGO);
        cobroCommand.setMedioPago(MedioPagoEnum.MERCADO_PAGO);

        return switch (command.getTipoPago()) {
            case SENA -> reservaAmenityCobroService.registrarSena(reserva.getReservaUuid(), cobroCommand);
            case TOTAL -> reservaAmenityCobroService.registrarTotal(reserva.getReservaUuid(), cobroCommand);
        };
    }

    @Transactional
    public ReservaAmenityCobroResultDTO reintentarPago(String reservaUuid, PortalReservaAmenityPagoCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_AMENITIES_RESERVE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(memberAuthService.getCurrentMemberOrThrow(company.getId()));
        memberAuthService.requirePortalEligible(titular);

        Reserva reserva = reservaRepository.findByUuidAndCompanyIdWithDetails(reservaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Reserva no encontrada",
                        "PortalAmenityReservaService | reservaUuid: " + reservaUuid));

        if (!reserva.getMember().getId().equals(titular.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tenés acceso a esta reserva",
                    "PortalAmenityReservaService | reservaUuid: " + reservaUuid
                            + " | titularId: " + titular.getId());
        }

        ReservaAmenityCobroCommand cobroCommand = new ReservaAmenityCobroCommand();
        cobroCommand.setCanalCobro(CanalCobroEnum.MERCADO_PAGO);
        cobroCommand.setMedioPago(MedioPagoEnum.MERCADO_PAGO);

        if (reserva.getEstado() == ReservaEstadoEnum.CONFIRMADA) {
            if (reserva.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException(HttpStatus.CONFLICT, "No hay ningún pago pendiente para esta reserva",
                        "PortalAmenityReservaService | reservaUuid: " + reservaUuid);
            }
            return reservaAmenityCobroService.registrarSaldo(reservaUuid, cobroCommand);
        }

        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "La reserva no admite un nuevo pago en su estado actual",
                    "PortalAmenityReservaService | reservaUuid: " + reservaUuid
                            + " | estado: " + reserva.getEstado());
        }

        if (command == null || command.getTipoPago() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Debe indicar tipoPago (SENA o TOTAL) mientras la reserva no tenga seña pagada",
                    "PortalAmenityReservaService | reservaUuid: " + reservaUuid);
        }

        return switch (command.getTipoPago()) {
            case SENA -> reservaAmenityCobroService.registrarSena(reservaUuid, cobroCommand);
            case TOTAL -> reservaAmenityCobroService.registrarTotal(reservaUuid, cobroCommand);
        };
    }
}
