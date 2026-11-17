package com.blue.open.Code.Event;

import com.blue.open.Code.Service.Categorias.CategoriaBajaDesasignacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CategoriaBajaEventListener {

    @Autowired
    private CategoriaBajaDesasignacionService categoriaBajaDesasignacionService;

    @Async("AsyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoriaBaja(CategoriaBajaEvent event) {
        try {
            categoriaBajaDesasignacionService.desasignarPorBaja(
                    event.getCompanyId(),
                    event.getCategoriaId(),
                    event.getCategoriaUuid());
        } catch (Exception ex) {
            log.error(
                    "Error al desasignar categoría tras baja | companyId={} categoriaId={} uuid={}",
                    event.getCompanyId(),
                    event.getCategoriaId(),
                    event.getCategoriaUuid(),
                    ex);
        }
    }
}
