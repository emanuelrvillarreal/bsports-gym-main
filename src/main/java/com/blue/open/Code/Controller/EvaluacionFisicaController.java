package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EvaluacionFisicaDTO;
import com.blue.open.Code.Service.Evaluaciones.EvaluacionFisicaService;
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
 * Evaluaciones fisicas (modulo opcional por club). ADMIN y, si el club lo habilita, PROFESOR para sus
 * alumnos. Permiso PHYSICAL_EVALUATION_MANAGE; el alcance lo valida el servicio.
 */
@RestController
@RequestMapping("evaluaciones")
@SecurityRequirement(name = "bearerAuth")
public class EvaluacionFisicaController {

    @Autowired
    private EvaluacionFisicaService service;

    @GetMapping("/acceso")
    @Operation(summary = "Estado del módulo para el usuario", description = "Si está activo, si puede registrar y (ADMIN) la configuración.")
    public ResponseEntity<EvaluacionFisicaDTO.Acceso> acceso() {
        return ResponseEntity.ok(service.acceso());
    }

    @PutMapping("/config")
    @Operation(summary = "Configurar el módulo", description = "Activar o desactivar y habilitar a los profesores. Solo ADMIN.")
    public ResponseEntity<EvaluacionFisicaDTO.Acceso> config(@RequestBody EvaluacionFisicaDTO.Config request) {
        return ResponseEntity.ok(service.guardarConfig(request));
    }

    @GetMapping("/socios")
    @Operation(summary = "Socios evaluables", description = "Con su última evaluación y la próxima prevista.")
    public ResponseEntity<List<EvaluacionFisicaDTO.Fila>> socios(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.socios(search));
    }

    @GetMapping("/socios/{memberUuid}")
    @Operation(summary = "Historial de evaluaciones de un socio")
    public ResponseEntity<EvaluacionFisicaDTO.Historial> historial(@PathVariable String memberUuid) {
        return ResponseEntity.ok(service.historial(memberUuid));
    }

    @PostMapping("/socios/{memberUuid}")
    @Operation(summary = "Registrar evaluación", description = "El IMC se calcula con peso y altura.")
    public ResponseEntity<EvaluacionFisicaDTO.Historial> crear(@PathVariable String memberUuid,
                                                              @Valid @RequestBody EvaluacionFisicaDTO.Command command) {
        return ResponseEntity.ok(service.crear(memberUuid, command));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Corregir evaluación", description = "ADMIN o el profesor que la cargó.")
    public ResponseEntity<EvaluacionFisicaDTO.Historial> editar(@PathVariable String uuid,
                                                               @Valid @RequestBody EvaluacionFisicaDTO.Command command) {
        return ResponseEntity.ok(service.editar(uuid, command));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar evaluación", description = "Baja lógica. ADMIN o el profesor que la cargó.")
    public ResponseEntity<EvaluacionFisicaDTO.Historial> borrar(@PathVariable String uuid) {
        return ResponseEntity.ok(service.borrar(uuid));
    }
}
