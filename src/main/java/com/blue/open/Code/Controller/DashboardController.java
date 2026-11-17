package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.DashboardDTO;
import com.blue.open.Code.Service.Dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores del panel de administracion. */
@RestController
@RequestMapping("dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    @Autowired
    private DashboardService service;

    @GetMapping("/administradores/resumen")
    @Operation(summary = "Resumen operativo", description = "Socios, actividades, accesos y reservas. El bloque financiero solo para ADMIN. Permiso DASHBOARD_VIEW (ADMIN y recepción).")
    public ResponseEntity<DashboardDTO.Resumen> resumen() {
        return ResponseEntity.ok(service.resumen());
    }

    @GetMapping("/general")
    @Operation(summary = "Dashboard institucional", description = "Socios, actividades, asistencia, deportivo, economía y alertas, con evolución de 12 meses. Solo ADMIN.")
    public ResponseEntity<DashboardDTO.General> general() {
        return ResponseEntity.ok(service.general());
    }
}
