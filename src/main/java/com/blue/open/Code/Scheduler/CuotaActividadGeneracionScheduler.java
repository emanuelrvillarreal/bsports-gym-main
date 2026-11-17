package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.DTO.CuotaActividadGeneracionResultDTO;
import com.blue.open.Code.Service.Tesoreria.CuotaActividadGeneracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Component
@Slf4j
public class CuotaActividadGeneracionScheduler {

    @Autowired
    private CuotaActividadGeneracionService cuotaActividadGeneracionService;

    @Value("${tesoreria.cuota-actividad.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${tesoreria.cuota-actividad.cron:0 30 0 * * *}")
    public void generarCuotasActividadDiarias() {
        if (!enabled) {
            log.debug("Job cuotas actividad deshabilitado (tesoreria.cuota-actividad.enabled=false)");
            return;
        }

        log.info("Inicio job cuotas actividad diarias");
        CuotaActividadGeneracionResultDTO result =
                cuotaActividadGeneracionService.generarHasta(LocalDate.now());
        log.info("Fin job cuotas actividad: {}", result);
    }
}
