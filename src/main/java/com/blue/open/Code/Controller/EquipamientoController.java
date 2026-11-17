package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EquipamientoDTO;
import com.blue.open.Code.Service.Equipamiento.EquipamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Equipamiento e infraestructura del club: inventario y mantenimientos. Solo ADMIN (EQUIPMENT_MANAGE). */
@RestController
@RequestMapping("equipamiento")
@SecurityRequirement(name = "bearerAuth")
public class EquipamientoController {

    @Autowired
    private EquipamientoService service;

    @PostMapping("/paginate")
    @Operation(summary = "Inventario", description = "Con búsqueda, estado, disciplina y alerta de mantenimiento (VENCIDO / PROXIMO).")
    public ResponseEntity<Page<EquipamientoDTO.Fila>> listar(@Valid @RequestBody EquipamientoDTO.PaginateRequest request) {
        return ResponseEntity.ok(service.listar(request));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen del inventario", description = "Ítems, unidades, en reparación, fuera de servicio, alertas y costo del año.")
    public ResponseEntity<EquipamientoDTO.Resumen> resumen() {
        return ResponseEntity.ok(service.resumen());
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Detalle con historial de mantenimientos")
    public ResponseEntity<EquipamientoDTO.Detalle> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(service.obtener(uuid));
    }

    @PostMapping
    @Operation(summary = "Alta de equipamiento")
    public ResponseEntity<EquipamientoDTO.Detalle> crear(@Valid @RequestBody EquipamientoDTO.Command command) {
        return ResponseEntity.ok(service.crear(command));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Editar equipamiento", description = "Para dar de baja un equipo usar el estado BAJA (queda en el historial).")
    public ResponseEntity<EquipamientoDTO.Detalle> editar(@PathVariable String uuid, @Valid @RequestBody EquipamientoDTO.Command command) {
        return ResponseEntity.ok(service.editar(uuid, command));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar un alta hecha por error", description = "Baja lógica.")
    public ResponseEntity<Void> eliminar(@PathVariable String uuid) {
        service.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/mantenimientos")
    @Operation(summary = "Registrar mantenimiento", description = "Opcionalmente registra el costo como egreso en tesorería.")
    public ResponseEntity<EquipamientoDTO.Detalle> mantenimiento(@PathVariable String uuid,
                                                                 @Valid @RequestBody EquipamientoDTO.MantenimientoCommand command) {
        return ResponseEntity.ok(service.registrarMantenimiento(uuid, command));
    }

    @DeleteMapping("/mantenimientos/{uuid}")
    @Operation(summary = "Eliminar mantenimiento cargado por error", description = "No permitido si generó un egreso (hay que anularlo primero).")
    public ResponseEntity<EquipamientoDTO.Detalle> eliminarMantenimiento(@PathVariable String uuid) {
        return ResponseEntity.ok(service.eliminarMantenimiento(uuid));
    }
}
