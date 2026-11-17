package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Service.Membresias.MembresiaRenovacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Renueva las membresías con renovación automática que vencen en los próximos días. */
@Component
@Slf4j
public class MembresiaRenovacionScheduler {

    @Autowired
    private MembresiaRenovacionService renovacionService;

    @Value("${membresias.renovacion.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${membresias.renovacion.cron:0 15 1 * * *}")
    public void renovar() {
        if (!enabled) return;
        int renovadas = renovacionService.renovarPendientes();
        log.info("Membresías renovadas automáticamente: {}", renovadas);
    }
}
