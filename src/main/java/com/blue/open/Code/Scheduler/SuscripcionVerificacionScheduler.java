package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.DTO.SuscripcionVerificacionResumenDTO;
import com.blue.open.Code.Service.SuscripcionVerificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class SuscripcionVerificacionScheduler {

    @Autowired
    private SuscripcionVerificacionService suscripcionVerificacionService;

    @Value("${crm.suscripcion-verificacion.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${crm.suscripcion-verificacion.cron:0 0 3 * * *}")
    public void verificarSuscripcionesDiario() {
        if (!enabled) {
            log.debug("Job verificacion de suscripciones deshabilitado "
                    + "(crm.suscripcion-verificacion.enabled=false)");
            return;
        }

        log.info("Inicio job verificacion de suscripciones");
        SuscripcionVerificacionResumenDTO resumen = suscripcionVerificacionService
                .verificarTodas(LocalDate.now());
        log.info("Fin job verificacion de suscripciones: {} facturas marcadas VENCIDA | {} clubes evaluados",
                resumen.getFacturasVencidas(), resumen.getClubesEvaluados());
    }
}
