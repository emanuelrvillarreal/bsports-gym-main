package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.CuotaVencimientoResultDTO;
import com.blue.open.Code.Entity.DTO.SuscripcionVerificacionResumenDTO;
import com.blue.open.Code.Service.CrmCatalogoSyncService;
import com.blue.open.Code.Service.SuscripcionVerificacionService;
import com.blue.open.Code.Service.Tesoreria.CuotaVencimientoService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Disparo manual de los jobs programados de facturacion/CRM, solo SYSADMIN.
 * <p>
 * Cada endpoint ejecuta el mismo service que su scheduler correspondiente
 * (SuscripcionVerificacionScheduler, CrmCatalogoSyncScheduler), sin esperar al horario
 * programado — util para soporte y para probar sin esperar al cron.
 */
@RestController
@RequestMapping("sysadmin/jobs")
public class SysadminJobsController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SuscripcionVerificacionService suscripcionVerificacionService;

    @Autowired
    private CrmCatalogoSyncService crmCatalogoSyncService;

    @Autowired
    private CuotaVencimientoService cuotaVencimientoService;

    /**
     * Marca las facturas vencidas, verifica contra el CRM el estado de la suscripcion de cada club
     * y ajusta su acceso a la plataforma.
     */
    @PostMapping("/verificar-suscripciones")
    public ResponseEntity<SuscripcionVerificacionResumenDTO> ejecutarVerificacionSuscripciones() {
        this.authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(this.suscripcionVerificacionService.verificarTodas(LocalDate.now()));
    }

    /** Sincroniza el catalogo de planes y productos del CRM contra plan_crm/productos_crm. */
    @PostMapping("/catalogo-sync")
    public ResponseEntity<Void> ejecutarCatalogoSync() {
        this.authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        this.crmCatalogoSyncService.sincronizar();
        return ResponseEntity.noContent().build();
    }

    /** Marca como vencidas las cuotas pendientes cuya fecha de vencimiento ya paso, para todas las companies activas. */
    @PostMapping("/verificar-cuotas-vencidas")
    public ResponseEntity<CuotaVencimientoResultDTO> ejecutarVerificacionCuotasVencidas() {
        this.authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(this.cuotaVencimientoService.marcarVencidasGlobal(LocalDate.now()));
    }
}
