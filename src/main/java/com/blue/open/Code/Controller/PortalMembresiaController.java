package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Service.Membresias.MembresiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** "Mis membresías" en el portal. Contratar y renovar generan una cuota que se paga como cualquier otra. */
@RestController
@RequestMapping("portal/member/membresias")
@SecurityRequirement(name = "bearerAuth")
public class PortalMembresiaController {

    @Autowired
    private MembresiaService membresiaService;

    @GetMapping
    @Operation(summary = "Mis membresías", description = "Propias, y las del grupo si es titular, con los pases que se pueden contratar online.")
    public ResponseEntity<MembresiaDTO.Portal> mias() {
        return ResponseEntity.ok(membresiaService.portal());
    }

    @PostMapping
    @Operation(summary = "Contratar pase", description = "Solo titular y solo pases habilitados para el portal.")
    public ResponseEntity<MembresiaDTO.Membresia> contratar(@Valid @RequestBody MembresiaDTO.PortalContratarCommand command) {
        return ResponseEntity.ok(membresiaService.portalContratar(command));
    }

    @PostMapping("/{uuid}/renovar")
    @Operation(summary = "Renovar pase")
    public ResponseEntity<MembresiaDTO.Membresia> renovar(@PathVariable String uuid) {
        return ResponseEntity.ok(membresiaService.portalRenovar(uuid));
    }
}
