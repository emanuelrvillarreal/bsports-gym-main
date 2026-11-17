package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Service.Actividades.SlotGeneracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class SlotExtensionScheduler {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private SlotGeneracionService slotGeneracionService;

    @Value("${actividades.slot-extension.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${actividades.slot-extension.cron:0 0 1 * * *}")
    public void extenderVentanaDiaria() {
        if (!enabled) {
            log.debug("Job extensión slots deshabilitado (actividades.slot-extension.enabled=false)");
            return;
        }

        LocalDate diaObjetivo = LocalDate.now().plusMonths(3);
        List<Actividad> actividades = actividadRepository.findByStatus(STATUS_ACTIVE);
        int creados = 0;
        for (Actividad actividad : actividades) {
            if (slotGeneracionService.generarDiaExtension(actividad, diaObjetivo)) {
                creados++;
            }
        }
        log.info("Fin job extensión slots: {} slots creados para día {}", creados, diaObjetivo);
    }
}
