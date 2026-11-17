package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.EvaluacionFisicaDTO;
import com.blue.open.Code.Service.Evaluaciones.EvaluacionFisicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Portal del socio: "Mis evaluaciones" (propias o de un familiar de su grupo). Solo lectura. */
@RestController
@RequestMapping("portal/member/evaluaciones")
@SecurityRequirement(name = "bearerAuth")
public class PortalEvaluacionFisicaController {

    @Autowired
    private EvaluacionFisicaService service;

    @GetMapping
    @Operation(summary = "Mis evaluaciones físicas", description = "Historial completo; vacío si el club no tiene el módulo activo.")
    public ResponseEntity<EvaluacionFisicaDTO.Historial> misEvaluaciones(@RequestParam(required = false) String memberUuid) {
        return ResponseEntity.ok(service.delSocio(memberUuid));
    }
}
