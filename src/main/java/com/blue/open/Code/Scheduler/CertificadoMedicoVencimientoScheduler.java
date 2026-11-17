package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Service.CertificadoMedicoVencimientoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class CertificadoMedicoVencimientoScheduler {

    @Autowired
    private CertificadoMedicoVencimientoService certificadoMedicoVencimientoService;

    @Value("${certificados.certificado-medico-vencimiento.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${certificados.certificado-medico-vencimiento.cron:0 0 2 * * *}")
    public void marcarCertificadosVencidosDiario() {
        if (!enabled) {
            log.debug("Job certificado médico vencimiento deshabilitado (certificados.certificado-medico-vencimiento.enabled=false)");
            return;
        }

        log.info("Inicio job vencimiento de certificados médicos");
        int marcados = certificadoMedicoVencimientoService.marcarVencidos(LocalDate.now().minusDays(1));
        log.info("Fin job vencimiento de certificados médicos: {} marcados como VENCIDO", marcados);
    }
}
