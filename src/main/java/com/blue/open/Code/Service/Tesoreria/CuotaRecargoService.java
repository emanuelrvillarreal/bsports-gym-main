package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Aplica el recargo por pago fuera de término a las cuotas impagas. La cuota social usa la política del club y
 * las cuotas de actividad la de su actividad. Una cuota recibe el recargo una sola vez, cuando pasaron
 * vencimiento + días de gracia sin pagarse.
 */
@Service
public class CuotaRecargoService {

    private static final int STATUS_ACTIVE = 0;

    private static final List<CuotaEstadoEnum> ESTADOS_IMPAGOS =
            List.of(CuotaEstadoEnum.PENDIENTE, CuotaEstadoEnum.VENCIDA);

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    /** Recorre todos los clubes activos. Responde la cantidad de cuotas recargadas. */
    @Transactional
    public int aplicarGlobal(LocalDate hoy) {
        LocalDate fecha = hoy != null ? hoy : LocalDate.now();
        int total = 0;
        for (Company company : companyRepository.findByStatus(STATUS_ACTIVE)) {
            total += aplicarPorCompany(company, fecha);
        }
        return total;
    }

    @Transactional
    public int aplicarPorCompany(Company company, LocalDate hoy) {
        // Candidatas: vencidas antes de hoy. Los días de gracia de cada política los evalúa la propia cuota.
        LocalDate limite = hoy.minusDays(1);
        int aplicadas = 0;

        if (company.getRecargo().estaActivo()) {
            aplicadas += aplicar(company, hoy,
                    cuotaRepository.findSocialesImpagasParaRecargo(company.getId(), ESTADOS_IMPAGOS, limite));
        }
        aplicadas += aplicar(company, hoy,
                cuotaRepository.findActividadImpagasParaRecargo(company.getId(), ESTADOS_IMPAGOS, limite));
        return aplicadas;
    }

    private int aplicar(Company company, LocalDate hoy, List<Cuota> cuotas) {
        int aplicadas = 0;
        for (Cuota cuota : cuotas) {
            if (cuota.aplicarRecargoSiCorresponde(company, hoy)) {
                cuotaRepository.save(cuota);
                aplicadas++;
            }
        }
        return aplicadas;
    }
}
