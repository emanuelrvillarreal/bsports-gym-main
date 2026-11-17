package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.CuotaVencimientoResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Event.CuotasVencidasProcesadasEvent;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CuotaVencimientoService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public int marcarVencidasPorCompany(Long companyId, LocalDate fechaCorte) {
        LocalDate corte = fechaCorte != null ? fechaCorte : LocalDate.now();
        return cuotaRepository.marcarPendientesVencidas(
                companyId,
                CuotaEstadoEnum.PENDIENTE,
                CuotaEstadoEnum.VENCIDA,
                corte);
    }

    @Transactional
    public CuotaVencimientoResultDTO marcarVencidasGlobal(LocalDate fechaCorte) {
        LocalDate corte = fechaCorte != null ? fechaCorte : LocalDate.now();
        List<Company> companies = companyRepository.findByStatus(STATUS_ACTIVE);

        CuotaVencimientoResultDTO result = new CuotaVencimientoResultDTO();
        result.setFechaCorte(corte);
        result.setCompaniesProcessed(companies.size());

        int totalMarcadas = 0;
        for (Company company : companies) {
            totalMarcadas += marcarVencidasPorCompany(company.getId(), corte);
        }
        result.setCuotasMarcadasVencidas(totalMarcadas);

        eventPublisher.publishEvent(new CuotasVencidasProcesadasEvent(
                corte, companies.size(), totalMarcadas));

        return result;
    }
}
