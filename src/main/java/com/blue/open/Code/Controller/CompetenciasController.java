package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.CompetenciasDTO;
import com.blue.open.Code.Service.Competencias.CompetenciasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Competencias: equipos y planteles, torneos, partidos y estadisticas. ADMIN (TEAMS_MANAGE) gestiona;
 * el profesor consulta sus equipos en /portal/profesor/equipos (TEAMS_VIEW_ASSIGNED).
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
public class CompetenciasController {

    @Autowired
    private CompetenciasService service;

    // ---------------------------------------------------------------- Apoyo

    @GetMapping("competencias/opciones")
    @Operation(summary = "Opciones para formularios", description = "Profesores, categorías, actividades y equipos del club.")
    public ResponseEntity<CompetenciasDTO.Opciones> opciones() {
        return ResponseEntity.ok(service.opciones());
    }

    @GetMapping("competencias/socios")
    @Operation(summary = "Socios para sumar a un plantel")
    public ResponseEntity<List<CompetenciasDTO.SocioOpcion>> socios(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.socios(search));
    }

    // ---------------------------------------------------------------- Equipos

    @PostMapping("competencias/equipos/listar")
    @Operation(summary = "Equipos y planteles")
    public ResponseEntity<List<CompetenciasDTO.EquipoFila>> equipos(@Valid @RequestBody CompetenciasDTO.Filtro filtro) {
        return ResponseEntity.ok(service.listarEquipos(filtro));
    }

    @GetMapping("competencias/equipos/{uuid}")
    @Operation(summary = "Detalle de equipo", description = "Plantel actual, histórico con bajas y pases, estadísticas y torneos.")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> equipo(@PathVariable String uuid) {
        return ResponseEntity.ok(service.obtenerEquipo(uuid));
    }

    @PostMapping("competencias/equipos")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> crearEquipo(@Valid @RequestBody CompetenciasDTO.EquipoCommand command) {
        return ResponseEntity.ok(service.crearEquipo(command));
    }

    @PutMapping("competencias/equipos/{uuid}")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> editarEquipo(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.EquipoCommand command) {
        return ResponseEntity.ok(service.editarEquipo(uuid, command));
    }

    @PostMapping("competencias/equipos/{uuid}/jugadores")
    @Operation(summary = "Sumar jugador al plantel")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> agregarJugador(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.JugadorCommand command) {
        return ResponseEntity.ok(service.agregarJugador(uuid, command));
    }

    @PutMapping("competencias/jugadores/{uuid}")
    @Operation(summary = "Editar dorsal, posición o capitán")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> editarJugador(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.JugadorEdicionCommand command) {
        return ResponseEntity.ok(service.editarJugador(uuid, command));
    }

    @PostMapping("competencias/jugadores/{uuid}/baja")
    @Operation(summary = "Dar de baja del plantel", description = "Cierra el período; queda en el histórico.")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> bajaJugador(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.BajaJugadorCommand command) {
        return ResponseEntity.ok(service.bajaJugador(uuid, command));
    }

    @PostMapping("competencias/jugadores/{uuid}/pase")
    @Operation(summary = "Pase a otro equipo", description = "Cierra el período en el origen y abre uno en el destino.")
    public ResponseEntity<CompetenciasDTO.EquipoDetalle> pase(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.PaseCommand command) {
        return ResponseEntity.ok(service.pase(uuid, command));
    }

    // ---------------------------------------------------------------- Torneos

    @PostMapping("competencias/torneos/listar")
    @Operation(summary = "Torneos")
    public ResponseEntity<List<CompetenciasDTO.TorneoFila>> torneos(@Valid @RequestBody CompetenciasDTO.Filtro filtro) {
        return ResponseEntity.ok(service.listarTorneos(filtro));
    }

    @GetMapping("competencias/torneos/{uuid}")
    @Operation(summary = "Detalle de torneo", description = "Participantes, partidos, tabla de posiciones y goleadores.")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> torneo(@PathVariable String uuid) {
        return ResponseEntity.ok(service.obtenerTorneo(uuid));
    }

    @PostMapping("competencias/torneos")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> crearTorneo(@Valid @RequestBody CompetenciasDTO.TorneoCommand command) {
        return ResponseEntity.ok(service.crearTorneo(command));
    }

    @PutMapping("competencias/torneos/{uuid}")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> editarTorneo(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.TorneoCommand command) {
        return ResponseEntity.ok(service.editarTorneo(uuid, command));
    }

    @PostMapping("competencias/torneos/{uuid}/participantes")
    @Operation(summary = "Sumar participante", description = "Un equipo del club o un rival externo.")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> agregarParticipante(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.ParticipanteCommand command) {
        return ResponseEntity.ok(service.agregarParticipante(uuid, command));
    }

    @DeleteMapping("competencias/participantes/{uuid}")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> quitarParticipante(@PathVariable String uuid) {
        return ResponseEntity.ok(service.quitarParticipante(uuid));
    }

    @PostMapping("competencias/torneos/{uuid}/partidos")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> crearPartido(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.PartidoCommand command) {
        return ResponseEntity.ok(service.crearPartido(uuid, command));
    }

    @PutMapping("competencias/partidos/{uuid}")
    @Operation(summary = "Editar partido o cargar resultado")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> editarPartido(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.PartidoCommand command) {
        return ResponseEntity.ok(service.editarPartido(uuid, command));
    }

    @DeleteMapping("competencias/partidos/{uuid}")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> eliminarPartido(@PathVariable String uuid) {
        return ResponseEntity.ok(service.eliminarPartido(uuid));
    }

    @GetMapping("competencias/partidos/{uuid}/estadisticas")
    @Operation(summary = "Estadísticas individuales del partido")
    public ResponseEntity<CompetenciasDTO.EstadisticasPartido> estadisticas(@PathVariable String uuid) {
        return ResponseEntity.ok(service.estadisticas(uuid));
    }

    @PutMapping("competencias/partidos/{uuid}/estadisticas")
    @Operation(summary = "Guardar estadísticas individuales", description = "Reemplaza las del partido.")
    public ResponseEntity<CompetenciasDTO.EstadisticasPartido> guardarEstadisticas(@PathVariable String uuid, @Valid @RequestBody CompetenciasDTO.EstadisticasCommand command) {
        return ResponseEntity.ok(service.guardarEstadisticas(uuid, command));
    }

    // ---------------------------------------------------------------- Profesor

    @GetMapping("portal/profesor/equipos")
    @Operation(summary = "Mis equipos", description = "Equipos donde el profesor es entrenador, con plantel y torneos.")
    public ResponseEntity<List<CompetenciasDTO.EquipoDetalle>> misEquipos() {
        return ResponseEntity.ok(service.misEquipos());
    }

    @GetMapping("portal/profesor/torneos/{uuid}")
    @Operation(summary = "Torneo de uno de mis equipos")
    public ResponseEntity<CompetenciasDTO.TorneoDetalle> torneoProfesor(@PathVariable String uuid) {
        return ResponseEntity.ok(service.torneoDelProfesor(uuid));
    }
}
