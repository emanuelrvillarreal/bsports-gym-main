package com.blue.open.Code.Event;

import com.blue.open.Code.Service.CuotaSocialRango.MemberCuotaSocialRangoRecalculoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CuotaSocialRangoCambiadoEventListener {

    @Autowired
    private MemberCuotaSocialRangoRecalculoService memberCuotaSocialRangoRecalculoService;

    @Async("AsyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuotaSocialRangoCambiado(CuotaSocialRangoCambiadoEvent event) {
        try {
            memberCuotaSocialRangoRecalculoService.recalcularPorCompany(event.getCompanyId());
        } catch (Exception ex) {
            log.error(
                    "Error al recalcular rangos de cuota social | companyId={} rangoId={} uuid={}",
                    event.getCompanyId(),
                    event.getCuotaSocialRangoId(),
                    event.getCuotaSocialRangoUuid(),
                    ex);
        }
    }
}
