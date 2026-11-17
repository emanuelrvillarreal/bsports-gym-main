package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CobroCuotaCommand;
import com.blue.open.Code.Entity.DTO.CobroCuotaResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.Tesoreria.Cobro.CanalCobroStrategyRegistry;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CobroCuotaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CanalCobroStrategyRegistry canalCobroStrategyRegistry;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CuotaRepository cuotaRepository;

    public CobroCuotaResultDTO registrarCobro(CobroCuotaCommand command) {
        User registradoPor = authService.valid(List.of(
                PermissionEnum.TREASURY_COLLECT,
                PermissionEnum.PORTAL_FEE_PAY_MERCADO_PAGO));

        Company company = authService.getCurrentCompanyOrThrow();
        CobroCuotaCommand normalized = normalizeCommand(command);

        boolean isTreasuryStaff = authService.validBoolean(List.of(PermissionEnum.TREASURY_COLLECT));
        if (!isTreasuryStaff) {
            normalized = prepararCobroPortalMercadoPago(normalized, company);
        }

        return canalCobroStrategyRegistry.get(normalized.getCanalCobro()).procesar(normalized, company, registradoPor);
    }

    private CobroCuotaCommand prepararCobroPortalMercadoPago(CobroCuotaCommand command, Company company) {
        Member titular = memberAuthService.requireTitular(
                memberAuthService.getCurrentMemberOrThrow(company.getId()));
        memberAuthService.requirePortalEligible(titular);

        if (command.getCanalCobro() != CanalCobroEnum.MERCADO_PAGO
                || command.getMedioPago() != MedioPagoEnum.MERCADO_PAGO) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Solo se permite cobro mediante Mercado Pago",
                    "CobroCuotaService | portal | canal: " + command.getCanalCobro()
                            + " | medio: " + command.getMedioPago());
        }

        validarCuotasDelGrupoTitular(titular, command.getCuotaUuids(), company.getId());

        CobroCuotaCommand portalCommand = new CobroCuotaCommand();
        portalCommand.setCuotaUuids(command.getCuotaUuids());
        portalCommand.setCanalCobro(CanalCobroEnum.MERCADO_PAGO);
        portalCommand.setMedioPago(MedioPagoEnum.MERCADO_PAGO);
        portalCommand.setFecha(command.getFecha());
        portalCommand.setObservaciones(command.getObservaciones());
        return portalCommand;
    }

    private void validarCuotasDelGrupoTitular(Member titular, List<String> cuotaUuids, Long companyId) {
        if (cuotaUuids.isEmpty()) {
            return;
        }
        Map<String, Cuota> cuotasByUuid = cuotaRepository
                .findByUuidInAndCompanyIdWithPersonas(cuotaUuids, companyId).stream()
                .collect(Collectors.toMap(Cuota::getUuid, Function.identity(), (a, b) -> a));

        for (String cuotaUuid : cuotaUuids) {
            Cuota cuota = cuotasByUuid.get(cuotaUuid);
            if (cuota == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "Cuota no encontrada",
                        "CobroCuotaService | cuotaUuid: " + cuotaUuid);
            }
            if (!titular.getId().equals(cuota.getTitular().getId())) {
                throw new CustomException(HttpStatus.FORBIDDEN,
                        "La cuota no pertenece a su grupo familiar",
                        "CobroCuotaService | cuotaUuid: " + cuotaUuid
                                + " | titularId: " + titular.getId());
            }
        }
    }

    private CobroCuotaCommand normalizeCommand(CobroCuotaCommand command) {
        CobroCuotaCommand normalized = new CobroCuotaCommand();
        normalized.setCanalCobro(command.getCanalCobro());
        normalized.setMedioPago(command.getMedioPago());
        normalized.setFecha(command.getFecha());
        normalized.setObservaciones(command.getObservaciones());
        normalized.setCuotaUuids(command.getCuotaUuids().stream()
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf)));
        return normalized;
    }
}
