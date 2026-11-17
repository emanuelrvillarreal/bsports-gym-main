package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EventoAccesoAltaCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoEdicionCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanResultDTO;
import com.blue.open.Code.Service.EventoAcceso.EventoAccesoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("evento-acceso")
@RequiredArgsConstructor
public class EventoAccesoController {

    private final EventoAccesoService eventoAccesoService;

    @PostMapping
    public ResponseEntity<EventoAccesoDetalleDTO> crear(@Valid @RequestBody EventoAccesoAltaCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoAccesoService.crear(command));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<EventoAccesoDetalleDTO>> paginate(
            @RequestBody(required = false) EventoAccesoPaginateRequestDTO request) {
        return ResponseEntity.ok(eventoAccesoService.paginar(request));
    }

    @GetMapping("/{eventoUuid}")
    public ResponseEntity<EventoAccesoDetalleDTO> obtenerPorUuid(@PathVariable String eventoUuid) {
        return ResponseEntity.ok(eventoAccesoService.obtenerPorUuid(eventoUuid));
    }

    @PutMapping("/{eventoUuid}")
    public ResponseEntity<EventoAccesoDetalleDTO> editar(
            @PathVariable String eventoUuid,
            @Valid @RequestBody EventoAccesoEdicionCommand command) {
        return ResponseEntity.ok(eventoAccesoService.editar(eventoUuid, command));
    }

    @DeleteMapping("/{eventoUuid}")
    public ResponseEntity<Void> eliminar(@PathVariable String eventoUuid) {
        eventoAccesoService.eliminar(eventoUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventoUuid}/scan")
    public ResponseEntity<EventoAccesoScanResultDTO> scan(
            @PathVariable String eventoUuid,
            @Valid @RequestBody EventoAccesoScanCommand command) {
        return ResponseEntity.ok(eventoAccesoService.scan(eventoUuid, command));
    }

    @PostMapping("/{eventoUuid}/cobro")
    public ResponseEntity<EventoAccesoCobroResultDTO> cobrar(
            @PathVariable String eventoUuid,
            @Valid @RequestBody EventoAccesoCobroCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventoAccesoService.cobrar(eventoUuid, command));
    }

    @PostMapping("/{eventoUuid}/historial/paginate")
    public ResponseEntity<Page<EventoAccesoHistorialDetalleDTO>> historial(
            @PathVariable String eventoUuid,
            @RequestBody(required = false) EventoAccesoHistorialPaginateRequestDTO request) {
        return ResponseEntity.ok(eventoAccesoService.historial(eventoUuid, request));
    }
}
