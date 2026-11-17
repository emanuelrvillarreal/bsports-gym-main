package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.DTO.CuotaSocialGeneracionResultDTO;
import com.blue.open.Code.Service.Tesoreria.CuotaSocialGeneracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class CuotaSocialGeneracionScheduler {

    @Autowired
    private CuotaSocialGeneracionService cuotaSocialGeneracionService;

    @Value("${tesoreria.cuota-social.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${tesoreria.cuota-social.cron:0 0 0 * * *}")
    public void generarCuotasSocialesDiarias() {
        if (!enabled) {
            log.debug("Job cuotas sociales deshabilitado (tesoreria.cuota-social.enabled=false)");
            return;
        }

        log.info("Inicio job cuotas sociales diarias");
        CuotaSocialGeneracionResultDTO result =
                cuotaSocialGeneracionService.generarHasta(LocalDate.now());
        log.info("Fin job cuotas sociales: {}", result);
    }
}
