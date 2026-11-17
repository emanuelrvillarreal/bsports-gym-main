package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.RutinaDTO;
import com.blue.open.Code.Service.Entrenamiento.EntrenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Portal del socio: "Mis entrenamientos" (propios o de un familiar de su grupo). Solo lectura. */
@RestController
@RequestMapping("portal/member/entrenamiento")
@SecurityRequirement(name = "bearerAuth")
public class PortalEntrenamientoController {

    @Autowired
    private EntrenamientoService service;

    @GetMapping
    @Operation(summary = "Mis rutinas", description = "Rutinas asignadas (activas e historial) con ejercicios y sesiones registradas.")
    public ResponseEntity<List<RutinaDTO.Portal>> misRutinas(@RequestParam(required = false) String memberUuid) {
        return ResponseEntity.ok(service.rutinasDelSocio(memberUuid));
    }
}
