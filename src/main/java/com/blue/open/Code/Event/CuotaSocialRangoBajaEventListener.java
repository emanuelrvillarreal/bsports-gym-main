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
public class CuotaSocialRangoBajaEventListener {

    @Autowired
    private MemberCuotaSocialRangoRecalculoService memberCuotaSocialRangoRecalculoService;

    @Async("AsyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuotaSocialRangoBaja(CuotaSocialRangoBajaEvent event) {
        try {
            memberCuotaSocialRangoRecalculoService.recalcularPorRangoBorrado(
                    event.getCompanyId(), event.getCuotaSocialRangoId());
        } catch (Exception ex) {
            log.error(
                    "Error al recalcular rangos de cuota social tras baja | companyId={} rangoId={} uuid={}",
                    event.getCompanyId(),
                    event.getCuotaSocialRangoId(),
                    event.getCuotaSocialRangoUuid(),
                    ex);
        }
    }
}
