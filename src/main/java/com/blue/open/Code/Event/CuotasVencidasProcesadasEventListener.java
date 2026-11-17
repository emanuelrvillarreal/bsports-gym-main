package com.blue.open.Code.Event;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Service.Tesoreria.MemberEstadoRecalculoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CuotasVencidasProcesadasEventListener {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MemberEstadoRecalculoService memberEstadoRecalculoService;

    @EventListener
    public void onCuotasVencidasProcesadas(CuotasVencidasProcesadasEvent event) {
        List<Company> companies = companyRepository.findByStatus(STATUS_ACTIVE);
        log.info("Recálculo post-vencimiento para {} clubs, fechaCorte={}",
                companies.size(), event.getFechaCorte());

        for (Company company : companies) {
            memberEstadoRecalculoService.recalcularPorCompany(company.getId(), event.getFechaCorte());
        }
    }
}
