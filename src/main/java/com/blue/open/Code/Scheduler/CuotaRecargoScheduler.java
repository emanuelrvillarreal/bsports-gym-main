package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Service.Tesoreria.CuotaRecargoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class CuotaRecargoScheduler {

    @Autowired
    private CuotaRecargoService cuotaRecargoService;

    @Value("${tesoreria.cuota-recargo.enabled:true}")
    private boolean enabled;

    // Después del job de vencimiento (01:00)
    @Scheduled(cron = "${tesoreria.cuota-recargo.cron:0 30 1 * * *}")
    public void aplicarRecargosDiario() {
        if (!enabled) {
            log.debug("Job recargo de cuotas deshabilitado (tesoreria.cuota-recargo.enabled=false)");
            return;
        }
        log.info("Inicio job recargo de cuotas vencidas");
        int aplicados = cuotaRecargoService.aplicarGlobal(LocalDate.now());
        log.info("Fin job recargo de cuotas vencidas: {} cuotas recargadas", aplicados);
    }
}
