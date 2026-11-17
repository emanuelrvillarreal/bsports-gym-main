package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.BalanceMesDTO;
import com.blue.open.Code.Entity.DTO.BalancePaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.BalanceResumenMesDTO;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BalanceQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<CuotaEstadoEnum> ESTADOS_POR_COBRAR = List.of(
            CuotaEstadoEnum.PENDIENTE,
            CuotaEstadoEnum.VENCIDA);

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Transactional(readOnly = true)
    public BalanceMesDTO obtenerMesActual(Long companyId) {
        return buildBalance(companyId, YearMonth.now());
    }

    @Transactional(readOnly = true)
    public Page<BalanceResumenMesDTO> paginarHistorico(BalancePaginateRequestDTO request, Long companyId) {
        BalancePaginateRequestDTO req = request != null ? request : new BalancePaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 12 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        List<YearMonth> periodos = listarPeriodosHistoricos(companyId);
        int from = (page - 1) * size;
        if (from >= periodos.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page - 1, size), periodos.size());
        }
        int to = Math.min(from + size, periodos.size());
        List<BalanceResumenMesDTO> content = periodos.subList(from, to).stream()
                .map(ym -> toResumen(buildBalance(companyId, ym)))
                .toList();
        return new PageImpl<>(content, PageRequest.of(page - 1, size), periodos.size());
    }

    private List<YearMonth> listarPeriodosHistoricos(Long companyId) {
        YearMonth actual = YearMonth.now();
        Set<YearMonth> periodos = new LinkedHashSet<>();
        for (Object[] row : movimientoRepository.findDistinctPeriodosFromMovimientos(companyId)) {
            addPeriodo(periodos, row);
        }
        for (LocalDate fechaGeneracion : cuotaRepository.findDistinctFechasGeneracion(companyId)) {
            periodos.add(YearMonth.from(fechaGeneracion));
        }
        return periodos.stream()
                .filter(ym -> !ym.equals(actual))
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private void addPeriodo(Set<YearMonth> periodos, Object[] row) {
        if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
            return;
        }
        int anio = ((Number) row[0]).intValue();
        int mes = ((Number) row[1]).intValue();
        if (mes >= 1 && mes <= 12) {
            periodos.add(YearMonth.of(anio, mes));
        }
    }

    private BalanceMesDTO buildBalance(Long companyId, YearMonth periodo) {
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        BigDecimal ingresos = movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                companyId, MovimientoTipoEnum.INGRESO, MovimientoEstadoEnum.ACTIVO, desde, hasta);
        BigDecimal egresos = movimientoRepository.sumMontoByCompanyTipoEstadoAndFechaBetween(
                companyId, MovimientoTipoEnum.EGRESO, MovimientoEstadoEnum.ACTIVO, desde, hasta);
        BigDecimal porCobrar = cuotaRepository.sumMontoPorCobrarByPeriodo(
                companyId,
                ESTADOS_POR_COBRAR,
                desde,
                hasta);

        ingresos = zeroIfNull(ingresos);
        egresos = zeroIfNull(egresos);
        porCobrar = zeroIfNull(porCobrar);

        BalanceMesDTO dto = new BalanceMesDTO();
        dto.setPeriodo(periodo);
        dto.setMes(periodo.getMonthValue());
        dto.setAnio(periodo.getYear());
        dto.setIngresosTotales(ingresos);
        dto.setEgresosTotales(egresos);
        dto.setPorCobrar(porCobrar);
        dto.setIngresosNetos(ingresos.subtract(egresos));
        dto.setIngresosEstimados(ingresos.add(porCobrar));
        return dto;
    }

    private BalanceResumenMesDTO toResumen(BalanceMesDTO balance) {
        BalanceResumenMesDTO resumen = new BalanceResumenMesDTO();
        resumen.setPeriodo(balance.getPeriodo());
        resumen.setMes(balance.getMes());
        resumen.setAnio(balance.getAnio());
        resumen.setIngresosTotales(balance.getIngresosTotales());
        resumen.setEgresosTotales(balance.getEgresosTotales());
        resumen.setPorCobrar(balance.getPorCobrar());
        resumen.setIngresosNetos(balance.getIngresosNetos());
        resumen.setIngresosEstimados(balance.getIngresosEstimados());
        return resumen;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
