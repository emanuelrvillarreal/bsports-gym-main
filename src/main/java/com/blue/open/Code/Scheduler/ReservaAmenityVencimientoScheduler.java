package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Entity.DTO.ReservaVencimientoResultDTO;
import com.blue.open.Code.Service.Amenities.ReservaAmenityVencimientoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReservaAmenityVencimientoScheduler {

    @Autowired
    private ReservaAmenityVencimientoService reservaAmenityVencimientoService;

    @Value("${amenities.reserva-vencimiento.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${amenities.reserva-vencimiento.cron:0 0 * * * *}")
    public void procesarReservasVencidasSinSena() {
        if (!enabled) {
            log.debug("Job vencimiento reservas amenities deshabilitado");
            return;
        }

        log.info("Inicio job vencimiento reservas amenities sin seña");
        ReservaVencimientoResultDTO result = reservaAmenityVencimientoService.procesarVencidas();
        log.info("Fin job vencimiento reservas amenities: {}", result);
    }
}
