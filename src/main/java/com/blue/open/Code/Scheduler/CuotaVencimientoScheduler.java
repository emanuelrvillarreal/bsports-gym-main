package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.DTO.CuotaVencimientoResultDTO;
import com.blue.open.Code.Service.Tesoreria.CuotaVencimientoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class CuotaVencimientoScheduler {

    @Autowired
    private CuotaVencimientoService cuotaVencimientoService;

    @Value("${tesoreria.cuota-vencimiento.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${tesoreria.cuota-vencimiento.cron:0 0 1 * * *}")
    public void marcarCuotasVencidasDiario() {
        if (!enabled) {
            log.debug("Job cuota vencimiento deshabilitado (tesoreria.cuota-vencimiento.enabled=false)");
            return;
        }

        log.info("Inicio job vencimiento de cuotas");
        CuotaVencimientoResultDTO result = cuotaVencimientoService.marcarVencidasGlobal(LocalDate.now());
        log.info("Fin job vencimiento de cuotas: {}", result);
    }
}
