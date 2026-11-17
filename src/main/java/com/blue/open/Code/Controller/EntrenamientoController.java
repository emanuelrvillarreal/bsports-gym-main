package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EjercicioDTO;
import com.blue.open.Code.Entity.DTO.RutinaDTO;
import com.blue.open.Code.Entity.Enums.DificultadEjercicioEnum;
import com.blue.open.Code.Entity.Enums.TipoEjercicioEnum;
import com.blue.open.Code.Service.Entrenamiento.EntrenamientoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Entrenamiento: biblioteca de ejercicios y rutinas. Lo usan ADMIN (todo el club) y PROFESOR (sus
 * rutinas y sus alumnos), con el permiso TRAINING_MANAGE. El alcance se valida en el servicio.
 */
@RestController
@RequestMapping("entrenamiento")
@SecurityRequirement(name = "bearerAuth")
public class EntrenamientoController {

    @Autowired
    private EntrenamientoService service;

    // ---------------------------------------------------------------- Ejercicios

    @GetMapping("/ejercicios")
    @Operation(summary = "Biblioteca de ejercicios", description = "Ejercicios del club con búsqueda y filtros, paginados.")
    public ResponseEntity<Page<EjercicioDTO.Respuesta>> ejercicios(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String disciplina,
            @RequestParam(required = false) TipoEjercicioEnum tipo,
            @RequestParam(required = false) DificultadEjercicioEnum dificultad,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listarEjercicios(search, disciplina, tipo, dificultad, page, size));
    }

    @PostMapping("/ejercicios")
    @Operation(summary = "Crear ejercicio")
    public ResponseEntity<EjercicioDTO.Respuesta> crearEjercicio(@Valid @RequestBody EjercicioDTO.Command command) {
        return ResponseEntity.ok(service.crearEjercicio(command));
    }

    @PutMapping("/ejercicios/{uuid}")
    @Operation(summary = "Editar ejercicio", description = "Solo quien lo cargó o un ADMIN.")
    public ResponseEntity<EjercicioDTO.Respuesta> editarEjercicio(@PathVariable String uuid, @Valid @RequestBody EjercicioDTO.Command command) {
        return ResponseEntity.ok(service.editarEjercicio(uuid, command));
    }

    @DeleteMapping("/ejercicios/{uuid}")
    @Operation(summary = "Borrar ejercicio", description = "Baja lógica: las rutinas que ya lo usan lo conservan.")
    public ResponseEntity<Void> borrarEjercicio(@PathVariable String uuid) {
        service.borrarEjercicio(uuid);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Rutinas

    @PostMapping("/rutinas/paginate")
    @Operation(summary = "Listar rutinas")
    public ResponseEntity<Page<RutinaDTO.Fila>> rutinas(@Valid @RequestBody RutinaDTO.PaginateRequest request) {
        return ResponseEntity.ok(service.listarRutinas(request));
    }

    @GetMapping("/rutinas/{uuid}")
    @Operation(summary = "Detalle de rutina", description = "Días, ejercicios, asignaciones con sus sesiones e historial de cambios.")
    public ResponseEntity<RutinaDTO.Detalle> rutina(@PathVariable String uuid) {
        return ResponseEntity.ok(service.obtenerRutina(uuid));
    }

    @PostMapping("/rutinas")
    @Operation(summary = "Crear rutina", description = "Opcionalmente la asigna a socios en el mismo paso.")
    public ResponseEntity<RutinaDTO.Detalle> crearRutina(@Valid @RequestBody RutinaDTO.Command command) {
        return ResponseEntity.ok(service.crearRutina(command));
    }

    @PutMapping("/rutinas/{uuid}")
    @Operation(summary = "Editar rutina")
    public ResponseEntity<RutinaDTO.Detalle> editarRutina(@PathVariable String uuid, @Valid @RequestBody RutinaDTO.Command command) {
        return ResponseEntity.ok(service.editarRutina(uuid, command));
    }

    @PostMapping("/rutinas/{uuid}/reemplazar")
    @Operation(summary = "Reemplazar rutina", description = "Crea una versión nueva y pasa a ella a los socios que tenían la anterior activa.")
    public ResponseEntity<RutinaDTO.Detalle> reemplazar(@PathVariable String uuid, @Valid @RequestBody RutinaDTO.Command command) {
        return ResponseEntity.ok(service.reemplazarRutina(uuid, command));
    }

    @PostMapping("/rutinas/{uuid}/archivar")
    @Operation(summary = "Archivar rutina", description = "Finaliza sus asignaciones activas; queda en el historial.")
    public ResponseEntity<RutinaDTO.Detalle> archivar(@PathVariable String uuid) {
        return ResponseEntity.ok(service.archivarRutina(uuid));
    }

    @PostMapping("/rutinas/{uuid}/asignaciones")
    @Operation(summary = "Asignar rutina a socios")
    public ResponseEntity<RutinaDTO.Detalle> asignar(@PathVariable String uuid, @Valid @RequestBody RutinaDTO.AsignarCommand command) {
        return ResponseEntity.ok(service.asignar(uuid, command));
    }

    @PostMapping("/asignaciones/{uuid}/finalizar")
    @Operation(summary = "Finalizar la rutina de un socio")
    public ResponseEntity<RutinaDTO.Detalle> finalizar(@PathVariable String uuid) {
        return ResponseEntity.ok(service.finalizarAsignacion(uuid));
    }

    @PostMapping("/asignaciones/{uuid}/seguimientos")
    @Operation(summary = "Registrar sesión de entrenamiento")
    public ResponseEntity<RutinaDTO.Detalle> seguimiento(@PathVariable String uuid, @Valid @RequestBody RutinaDTO.SeguimientoCommand command) {
        return ResponseEntity.ok(service.registrarSeguimiento(uuid, command));
    }

    @GetMapping("/alumnos")
    @Operation(summary = "Socios asignables", description = "ADMIN: socios inscriptos en actividades. Profesor: alumnos de sus actividades.")
    public ResponseEntity<List<RutinaDTO.Alumno>> alumnos(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.alumnos(search));
    }
}
