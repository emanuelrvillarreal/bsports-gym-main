package com.blue.open.Code.Scheduler;

import com.blue.open.Code.Service.CrmCatalogoSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CrmCatalogoSyncScheduler {

    @Autowired
    private CrmCatalogoSyncService crmCatalogoSyncService;

    @Value("${crm.catalogo-sync.enabled:true}")
    private boolean enabled;

    @Value("${crm.catalogo-sync.on-startup:true}")
    private boolean onStartup;

    /**
     * Sincroniza el catalogo apenas arranca la aplicacion, sin esperar al cron de las 02:00.
     * <p>
     * plan_crm arranca vacia, y hay dos flujos que cortan si el plan no esta sincronizado: el alta
     * de club y el alta/cambio de plan de un club existente (CompanySuscripcionCrmService.resolverPlan
     * responde 409 a proposito, para no dejar la suscripcion apuntando a un plan incompleto). Sin
     * esto, un entorno recien levantado no puede dar de alta ningun club hasta la proxima corrida
     * del cron o hasta que alguien dispare POST /sysadmin/jobs/catalogo-sync a mano.
     * <p>
     * Corre en ApplicationReadyEvent, o sea con el servidor ya aceptando requests: si el CRM tarda
     * o no responde, no impide que la aplicacion atienda trafico. El error se loguea y se sigue —
     * el cron y los webhooks PLAN_CAMBIO/PRODUCTO_CAMBIO son la red de contencion.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAlArrancar() {
        if (!enabled || !onStartup) {
            log.debug("Sincronizacion inicial del catalogo CRM deshabilitada "
                    + "(crm.catalogo-sync.enabled/on-startup=false)");
            return;
        }

        log.info("Inicio sincronizacion inicial del catalogo CRM (planes y productos)");
        this.sincronizar("sincronizarAlArrancar");
        log.info("Fin sincronizacion inicial del catalogo CRM");
    }

    @Scheduled(cron = "${crm.catalogo-sync.cron:0 0 2 * * *}")
    public void sincronizarDiario() {
        if (!enabled) {
            log.debug("Job sincronizacion catalogo CRM deshabilitado (crm.catalogo-sync.enabled=false)");
            return;
        }

        log.info("Inicio job sincronizacion catalogo CRM (planes y productos)");
        this.sincronizar("sincronizarDiario");
        log.info("Fin job sincronizacion catalogo CRM");
    }

    /** Nunca propaga: una falla del CRM no puede tumbar el arranque ni la corrida del cron. */
    private void sincronizar(String origen) {
        try {
            crmCatalogoSyncService.sincronizar();
        } catch (Exception e) {
            log.error("CrmCatalogoSyncScheduler | {} | error: {}", origen, e.getMessage(), e);
        }
    }
}
