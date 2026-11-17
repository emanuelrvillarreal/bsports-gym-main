package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.CobroCuotaCommand;
import com.blue.open.Code.Entity.DTO.CobroCuotaItemResultDTO;
import com.blue.open.Code.Entity.DTO.CobroCuotaResultDTO;
import com.blue.open.Code.Entity.DTO.LiquidacionCuotaDetalle;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Service.Tesoreria.LiquidacionCuotaService;
import com.blue.open.Code.Service.Tesoreria.MemberEstadoRecalculoService;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CobroManualStrategy implements CanalCobroStrategy {

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private LiquidacionCuotaService liquidacionCuotaService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MemberEstadoRecalculoService memberEstadoRecalculoService;

    @Override
    public CanalCobroEnum getCanal() {
        return CanalCobroEnum.MANUAL;
    }

    @Override
    public CobroCuotaResultDTO procesar(CobroCuotaCommand command, Company company, User registradoPor) {
        List<String> cuotaUuids = dedupe(command.getCuotaUuids());
        Map<String, Cuota> cuotasByUuid = loadCuotasByUuid(cuotaUuids, company.getId());

        LiquidacionCuotaDetalle detalle = buildDetalle(command);
        CobroCuotaResultDTO result = new CobroCuotaResultDTO();
        Set<Long> titularIdsAfectados = new LinkedHashSet<>();

        for (String cuotaUuid : cuotaUuids) {
            CobroCuotaItemResultDTO item = procesarCuota(
                    cuotaUuid, cuotasByUuid, detalle, registradoPor, titularIdsAfectados);
            result.getItems().add(item);
            if (item.isExitoso()) {
                result.setExitosas(result.getExitosas() + 1);
            } else {
                result.setFallidas(result.getFallidas() + 1);
            }
        }

        if (!titularIdsAfectados.isEmpty()) {
            memberEstadoRecalculoService.recalcularGruposPorTitularIds(
                    company.getId(), titularIdsAfectados);
        }

        return result;
    }

    private CobroCuotaItemResultDTO procesarCuota(
            String cuotaUuid,
            Map<String, Cuota> cuotasByUuid,
            LiquidacionCuotaDetalle detalle,
            User registradoPor,
            Set<Long> titularIdsAfectados) {

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

        try {
            Movimiento movimiento = transactionTemplate.execute(status ->
                    liquidacionCuotaService.liquidarCuota(cuota, detalle, registradoPor));
            item.setExitoso(true);
            item.setMovimientoUuid(movimiento != null ? movimiento.getUuid() : null);
            titularIdsAfectados.add(cuota.getTitular().getId());
        } catch (CustomException ex) {
            item.setExitoso(false);
            item.setMensajeError(ex.getUserMessage());
        } catch (DataIntegrityViolationException ex) {
            item.setExitoso(false);
            item.setMensajeError("Conflicto de cobro concurrente");
        } catch (Exception ex) {
            item.setExitoso(false);
            item.setMensajeError("Error al registrar cobro");
        }

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

    private LiquidacionCuotaDetalle buildDetalle(CobroCuotaCommand command) {
        LiquidacionCuotaDetalle detalle = new LiquidacionCuotaDetalle();
        detalle.setMedioPago(command.getMedioPago());
        detalle.setFecha(command.getFecha() != null ? command.getFecha() : LocalDate.now());
        detalle.setObservaciones(command.getObservaciones());
        return detalle;
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
