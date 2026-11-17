package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EventoActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadEdicionCommand;
import com.blue.open.Code.Entity.DTO.EventoActividadEstadoCommand;
import com.blue.open.Code.Service.Actividades.EventoActividadAltaService;
import com.blue.open.Code.Service.Actividades.EventoActividadBajaService;
import com.blue.open.Code.Service.Actividades.EventoActividadDetalleQueryService;
import com.blue.open.Code.Service.Actividades.EventoActividadEdicionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("portal/profesor/evento-actividad")
public class PortalEventoActividadController {

    @Autowired
    private EventoActividadAltaService altaService;

    @Autowired
    private EventoActividadEdicionService edicionService;

    @Autowired
    private EventoActividadBajaService bajaService;

    @Autowired
    private EventoActividadDetalleQueryService detalleQueryService;

    @PostMapping
    public ResponseEntity<EventoActividadDetalleDTO> crear(@Valid @RequestBody EventoActividadAltaCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(altaService.crear(command, true));
    }

    @PutMapping("/{eventoUuid}")
    public ResponseEntity<EventoActividadDetalleDTO> editar(
            @PathVariable String eventoUuid,
            @Valid @RequestBody EventoActividadEdicionCommand command) {
        return ResponseEntity.ok(edicionService.editar(eventoUuid, command, true));
    }

    @PostMapping("/{eventoUuid}/estado")
    public ResponseEntity<EventoActividadDetalleDTO> cambiarEstado(
            @PathVariable String eventoUuid,
            @Valid @RequestBody EventoActividadEstadoCommand command) {
        return ResponseEntity.ok(bajaService.cambiarEstado(eventoUuid, command, true));
    }

    @DeleteMapping("/{eventoUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String eventoUuid) {
        bajaService.bajaLogica(eventoUuid, true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventoUuid}")
    public ResponseEntity<EventoActividadDetalleDTO> obtenerDetalle(@PathVariable String eventoUuid) {
        return ResponseEntity.ok(detalleQueryService.obtenerDetalle(eventoUuid));
    }
}
