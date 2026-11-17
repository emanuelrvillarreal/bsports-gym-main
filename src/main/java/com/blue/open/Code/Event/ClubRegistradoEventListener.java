package com.blue.open.Code.Event;

import com.blue.open.Code.Service.FacturaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ClubRegistradoEventListener {

    @Autowired
    private FacturaService facturaService;

    @Async("AsyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClubRegistrado(ClubRegistradoEvent event) {
        try {
            facturaService.asociarFacturasSinClub(event.getCompanyId(), event.getUuidCliente(), event.getCuit());
        } catch (Exception ex) {
            log.error(
                    "Error al asociar facturas al club registrado | companyId={} uuidCliente={} cuit={}",
                    event.getCompanyId(),
                    event.getUuidCliente(),
                    event.getCuit(),
                    ex);
        }
    }
}
